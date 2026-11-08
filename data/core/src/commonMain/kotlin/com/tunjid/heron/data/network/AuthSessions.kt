/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.network

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedState.AuthTokens.Authenticated.Bearer
import com.tunjid.heron.data.repository.SavedState.AuthTokens.Authenticated.DPoP
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.loadedSavedState
import dev.zacsweers.metro.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface AuthSessions {

    /**
     * [SessionContext] aware current auth.
     */
    suspend fun current(): SavedState.AuthTokens.Authenticated?

    suspend fun requestRefresh(
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
    ): RefreshResult

    suspend fun onRefreshRetrySuccess(
        tokens: SavedState.AuthTokens.Authenticated,
    )

    suspend fun setActive(
        auth: SavedState.AuthTokens?,
    )

    suspend fun switchTo(
        profileId: ProfileId,
    ): Boolean
}

internal enum class RefreshReason {
    Expiring,

    Rejected,

    Unauthorized,
}

internal sealed interface RefreshResult {

    data class Fresh(
        val tokens: SavedState.AuthTokens.Authenticated,
    ) : RefreshResult

    data object SessionEnded : RefreshResult

    data class Unavailable(
        val cause: Throwable,
    ) : RefreshResult

    data object Declined : RefreshResult
}

@Inject
internal class PersistedAuthSessions(
    @AppMainScope
    appMainScope: CoroutineScope,
    @IODispatcher
    ioDispatcher: CoroutineDispatcher,
    private val savedStateDataSource: SavedStateDataSource,
    private val tokenRefresher: TokenRefresher,
) : AuthSessions {

    // Independent scope for refreshes, so canceled requests can't
    // be abandoned after the server has rotated the refresh token.
    private val scope = appMainScope + ioDispatcher

    private val mutex = Mutex()

    // Everything below is guarded by the mutex.
    private var loaded = false
    private var active: SavedState.AuthTokens? = null

    // Tokens written in this process, by profile, where null means signed out.
    private val written = mutableMapOf<ProfileId, SavedState.AuthTokens.Authenticated?>()
    private val obtained = mutableMapOf<ProfileId, Obtained>()
    private val refreshes = mutableMapOf<String, Deferred<RefreshResult>>()

    // Refreshes that couldn't complete, by profile, to pace refreshing ahead of expiry.
    private val backoffs = mutableMapOf<ProfileId, Backoff>()

    override suspend fun current(): SavedState.AuthTokens.Authenticated? {
        val profileId = currentSessionContext()?.profileId
        val tokens = mutex.withLock {
            loadIfNeeded()
            tokensFor(
                profileId = profileId ?: active?.authenticatedProfileId ?: return null,
            )
        } ?: return null

        val expiresAt = tokens.accessTokenExpiresAt ?: return tokens
        if (expiresAt - Clock.System.now() > RefreshAheadBuffer) return tokens

        return when (
            val refresh = requestRefresh(
                used = tokens,
                reason = RefreshReason.Expiring,
            )
        ) {
            is RefreshResult.Fresh -> refresh.tokens
            RefreshResult.SessionEnded -> null
            // The access token may still be valid for a little while.
            is RefreshResult.Unavailable,
            RefreshResult.Declined,
            -> tokens
        }
    }

    // Refreshing must run to completion even if the caller is cancelled.
    override suspend fun requestRefresh(
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
    ): RefreshResult {
        val pending = mutex.withLock {
            loadIfNeeded()
            val latest = tokensFor(used.authProfileId) ?: return RefreshResult.SessionEnded
            // Another request already refreshed. Its refresh token has been spent.
            if (latest.accessToken != used.accessToken) return RefreshResult.Fresh(latest)

            refreshes[used.accessToken] ?: run {
                // The access token is still usable, so there's no need to retry refreshing
                // before it expires while the server can't be reached.
                if (reason == RefreshReason.Expiring && used.isBackingOff()) {
                    return RefreshResult.Declined
                }
                if (reason == RefreshReason.Unauthorized && used.wasObtainedRecently()) {
                    return RefreshResult.Declined
                }
                val rejections = when (reason) {
                    RefreshReason.Rejected -> used.rejectionsAfterThis()
                    RefreshReason.Expiring,
                    RefreshReason.Unauthorized,
                    -> 0
                }
                if (rejections >= MaxConsecutiveRejections) {
                    logcat(LogPriority.WARN) {
                        "Auth session for ${used.authProfileId.id} ended, its tokens were rejected $rejections times in a row"
                    }
                    write(
                        profileId = used.authProfileId,
                        tokens = null,
                    )
                    return RefreshResult.SessionEnded
                }
                scope.async {
                    refresh(
                        used = used,
                        reason = reason,
                        rejections = rejections,
                    )
                }.also { refreshes[used.accessToken] = it }
            }
        }
        return pending.await()
    }

    override suspend fun onRefreshRetrySuccess(
        tokens: SavedState.AuthTokens.Authenticated,
    ) = mutex.withLock {
        val profileId = tokens.authProfileId
        val current = obtained[profileId] ?: return@withLock
        if (current.accessToken != tokens.accessToken) return@withLock
        obtained[profileId] = current.copy(rejections = 0)
    }

    override suspend fun setActive(
        auth: SavedState.AuthTokens?,
    ) = mutex.withLock {
        loadIfNeeded()
        when (auth) {
            is SavedState.AuthTokens.Authenticated -> record(
                tokens = auth,
                rejections = 0,
            )
            null -> active?.authenticatedProfileId?.let { written[it] = null }
            is SavedState.AuthTokens.Guest,
            is SavedState.AuthTokens.Pending,
            -> Unit
        }
        active = auth
        savedStateDataSource.setAuth(auth)
    }

    override suspend fun switchTo(
        profileId: ProfileId,
    ): Boolean = mutex.withLock {
        loadIfNeeded()
        val tokens = tokensFor(profileId) ?: return@withLock false
        active = tokens
        savedStateDataSource.switchSession(
            profileId = profileId,
            freshAuth = tokens,
        )
        logcat(LogPriority.INFO) {
            "Switched auth session to ${profileId.id}"
        }
        true
    }

    private suspend fun refresh(
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
        rejections: Int,
    ): RefreshResult {
        val profileId = used.authProfileId
        logcat(LogPriority.INFO) {
            "Refreshing auth for ${profileId.id}, reason: $reason"
        }

        val outcome = try {
            RefreshResult.Fresh(tokenRefresher.refresh(used))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SessionEndedException) {
            logcat(LogPriority.WARN) {
                "Auth session for ${profileId.id} ended, refresh failed: ${exception.cause?.message}"
            }
            RefreshResult.SessionEnded
        } catch (exception: Throwable) {
            // Throwable, not Exception, so the refresh is always cleared from those in flight.
            logcat(LogPriority.WARN) {
                "Auth refresh for ${profileId.id} unavailable: ${exception.message}"
            }
            RefreshResult.Unavailable(exception)
        }

        return mutex.withLock {
            refreshes.remove(used.accessToken)
            when (outcome) {
                is RefreshResult.Unavailable -> backoffs[profileId] = Backoff.after(
                    accessToken = used.accessToken,
                    previous = backoffs[profileId],
                )
                else -> backoffs.remove(profileId)
            }
            val latest = tokensFor(profileId)
            // The session was replaced while refreshing, like by signing out or back in.
            if (latest?.accessToken != used.accessToken) {
                return@withLock latest?.let(RefreshResult::Fresh) ?: RefreshResult.SessionEnded
            }

            when (outcome) {
                is RefreshResult.Fresh -> write(
                    profileId = profileId,
                    tokens = outcome.tokens,
                    rejections = rejections,
                )
                RefreshResult.SessionEnded -> write(
                    profileId = profileId,
                    tokens = null,
                )
                is RefreshResult.Unavailable,
                RefreshResult.Declined,
                -> Unit
            }
            outcome
        }
    }

    private suspend fun loadIfNeeded() {
        if (loaded) return
        active = savedStateDataSource.loadedSavedState().auth
        loaded = true
    }

    private fun tokensFor(
        profileId: ProfileId,
    ): SavedState.AuthTokens.Authenticated? =
        if (profileId in written) written[profileId]
        else savedStateDataSource.savedState.value
            .profileData(profileId)
            ?.auth as? SavedState.AuthTokens.Authenticated

    private suspend fun write(
        profileId: ProfileId,
        tokens: SavedState.AuthTokens.Authenticated?,
        rejections: Int = 0,
    ) {
        if (tokens != null) record(
            tokens = tokens,
            rejections = rejections,
        )
        else written[profileId] = null
        if (active?.authProfileId == profileId) active = tokens
        savedStateDataSource.updateAuth(
            profileId = profileId,
            auth = tokens,
        )
    }

    private fun record(
        tokens: SavedState.AuthTokens.Authenticated,
        rejections: Int,
    ) {
        written[tokens.authProfileId] = tokens
        obtained[tokens.authProfileId] = Obtained(
            accessToken = tokens.accessToken,
            at = Clock.System.now(),
            rejections = rejections,
        )
    }
    private fun SavedState.AuthTokens.Authenticated.rejectionsAfterThis(): Int =
        obtainedWithin(RejectedSoonAfterBuffer)
            ?.let { it.rejections + 1 }
            ?: 0

    private fun SavedState.AuthTokens.Authenticated.wasObtainedRecently(): Boolean =
        obtainedWithin(RecentlyObtainedBuffer) != null

    private fun SavedState.AuthTokens.Authenticated.obtainedWithin(
        duration: Duration,
    ): Obtained? =
        obtained[authProfileId]
            ?.takeIf { it.accessToken == accessToken && Clock.System.now() - it.at < duration }

    private fun SavedState.AuthTokens.Authenticated.isBackingOff(): Boolean =
        backoffs[authProfileId]
            ?.let { it.accessToken == accessToken && Clock.System.now() < it.until }
            ?: false
}

private data class Obtained(
    val accessToken: String,
    val at: Instant,
    val rejections: Int,
)

/**
 * Refreshing tokens with [accessToken] failed [failures] times in a row, so refreshing them
 * ahead of expiry shouldn't be tried again until [until].
 */
private class Backoff(
    val accessToken: String,
    val failures: Int,
    val until: Instant,
) {
    companion object {
        fun after(
            accessToken: String,
            previous: Backoff?,
        ): Backoff {
            val failures = previous
                ?.takeIf { it.accessToken == accessToken }
                ?.failures
                ?.plus(1)
                ?: 1
            return Backoff(
                accessToken = accessToken,
                failures = failures,
                until = Clock.System.now() + minOf(
                    InitialRefreshBackoff * (1 shl (failures - 1).coerceAtMost(MaxBackoffDoublings)),
                    MaxRefreshBackoff,
                ),
            )
        }
    }
}

private val SavedState.AuthTokens.authenticatedProfileId: ProfileId?
    get() = (this as? SavedState.AuthTokens.Authenticated)?.authProfileId

private val SavedState.AuthTokens.Authenticated.accessToken: String
    get() = when (this) {
        is Bearer -> auth
        is DPoP -> auth
    }

private val SavedState.AuthTokens.Authenticated.accessTokenExpiresAt: Instant?
    get() = when (this) {
        is Bearer -> expiresAt
        is DPoP -> expiresAt
    }

private val RefreshAheadBuffer = 60.seconds
private val RecentlyObtainedBuffer = 30.seconds
private val RejectedSoonAfterBuffer = 5.minutes
private val InitialRefreshBackoff = 5.seconds
private val MaxRefreshBackoff = 45.seconds
private const val MaxBackoffDoublings = 5
private const val MaxConsecutiveRejections = 3
