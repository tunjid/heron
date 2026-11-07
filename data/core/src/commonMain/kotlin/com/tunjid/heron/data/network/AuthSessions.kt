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

    suspend fun refreshed(
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
    ): Refresh

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

internal sealed interface Refresh {

    data class Fresh(
        val tokens: SavedState.AuthTokens.Authenticated,
    ) : Refresh

    data object SessionEnded : Refresh

    data class Unavailable(
        val cause: Throwable,
    ) : Refresh

    data object Declined : Refresh
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
    private val obtainedAt = mutableMapOf<String, Instant>()
    private val refreshes = mutableMapOf<String, Deferred<Refresh>>()

    // Consecutive rejections of tokens soon after they were obtained, by profile.
    private val rejections = mutableMapOf<ProfileId, Int>()

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
            val refresh = refreshed(
                used = tokens,
                reason = RefreshReason.Expiring,
            )
        ) {
            is Refresh.Fresh -> refresh.tokens
            Refresh.SessionEnded -> null
            // The access token may still be valid for a little while.
            is Refresh.Unavailable,
            Refresh.Declined,
            -> tokens
        }
    }

    // Refreshing must run to completion even if the caller is cancelled.
    override suspend fun refreshed(
        used: SavedState.AuthTokens.Authenticated,
        reason: RefreshReason,
    ): Refresh {
        val pending = mutex.withLock {
            loadIfNeeded()
            val latest = tokensFor(used.authProfileId) ?: return Refresh.SessionEnded
            // Another request already refreshed. Its refresh token has been spent.
            if (latest.accessToken != used.accessToken) return Refresh.Fresh(latest)

            refreshes[used.accessToken] ?: run {
                if (reason == RefreshReason.Unauthorized && used.wasObtainedRecently()) {
                    return Refresh.Declined
                }
                if (reason == RefreshReason.Rejected && endsAfterRejecting(used)) {
                    write(
                        profileId = used.authProfileId,
                        tokens = null,
                    )
                    return Refresh.SessionEnded
                }
                scope.async {
                    refresh(
                        used = used,
                        reason = reason,
                    )
                }.also { refreshes[used.accessToken] = it }
            }
        }
        return pending.await()
    }

    override suspend fun setActive(
        auth: SavedState.AuthTokens?,
    ) = mutex.withLock {
        loadIfNeeded()
        when (auth) {
            is SavedState.AuthTokens.Authenticated -> {
                record(auth)
                rejections.remove(auth.authProfileId)
            }
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
    ): Refresh {
        val profileId = used.authProfileId
        logcat(LogPriority.INFO) {
            "Refreshing auth for ${profileId.id}, reason: $reason"
        }

        val outcome = try {
            Refresh.Fresh(tokenRefresher.refresh(used))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SessionEndedException) {
            logcat(LogPriority.WARN) {
                "Auth session for ${profileId.id} ended, refresh failed: ${exception.cause?.message}"
            }
            Refresh.SessionEnded
        } catch (exception: Throwable) {
            // Throwable, not Exception, so the refresh is always cleared from those in flight.
            logcat(LogPriority.WARN) {
                "Auth refresh for ${profileId.id} unavailable: ${exception.message}"
            }
            Refresh.Unavailable(exception)
        }

        return mutex.withLock {
            refreshes.remove(used.accessToken)
            val latest = tokensFor(profileId)
            // The session was replaced while refreshing, like by signing out or back in.
            if (latest?.accessToken != used.accessToken) {
                return@withLock latest?.let(Refresh::Fresh) ?: Refresh.SessionEnded
            }

            when (outcome) {
                is Refresh.Fresh -> write(
                    profileId = profileId,
                    tokens = outcome.tokens,
                )
                Refresh.SessionEnded -> write(
                    profileId = profileId,
                    tokens = null,
                )
                is Refresh.Unavailable,
                Refresh.Declined,
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
    ) {
        if (tokens != null) record(tokens) else written[profileId] = null
        if (active?.authProfileId == profileId) active = tokens
        savedStateDataSource.updateAuth(
            profileId = profileId,
            auth = tokens,
        )
    }

    private fun record(
        tokens: SavedState.AuthTokens.Authenticated,
    ) {
        written[tokens.authProfileId] = tokens
        obtainedAt[tokens.accessToken] = Clock.System.now()
    }

    /**
     * Counts a rejection of [used], returning true if the session should end instead of being
     * refreshed: its tokens keep being rejected soon after being obtained, so refreshing again
     * won't help.
     */
    private fun endsAfterRejecting(
        used: SavedState.AuthTokens.Authenticated,
    ): Boolean {
        val profileId = used.authProfileId
        val count = when {
            used.wasObtainedWithin(RejectedSoonAfterBuffer) -> rejections.getOrElse(profileId) { 0 } + 1
            else -> 0
        }
        if (count < MaxConsecutiveRejections) {
            rejections[profileId] = count
            return false
        }
        rejections.remove(profileId)
        logcat(LogPriority.WARN) {
            "Auth session for ${profileId.id} ended, its tokens were rejected $count times in a row"
        }
        return true
    }

    private fun SavedState.AuthTokens.Authenticated.wasObtainedRecently(): Boolean =
        wasObtainedWithin(RecentlyObtainedBuffer)

    private fun SavedState.AuthTokens.Authenticated.wasObtainedWithin(
        duration: Duration,
    ): Boolean =
        obtainedAt[accessToken]
            ?.let { Clock.System.now() - it < duration }
            ?: false
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
private const val MaxConsecutiveRejections = 3
