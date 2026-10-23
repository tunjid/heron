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

package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.SessionSwitchException
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.network.IdentityResolver
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.SessionManager
import com.tunjid.heron.data.network.oauth.OauthRedirect
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.signedInUserRefresher.SignedInUserRefresher
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import sh.christian.ozone.api.Handle

interface AuthRepository {
    val isSignedIn: Flow<Boolean>

    val isGuest: Flow<Boolean>

    val signedInUser: Flow<Profile?>

    val pastSessions: Flow<List<SessionSummary>>

    fun isSignedInProfile(id: ProfileId): Flow<Boolean>

    suspend fun oauthRequestUri(
        request: OauthUriRequest,
    ): Result<GenericUri>

    suspend fun createSession(
        request: SessionRequest,
    ): Outcome

    suspend fun switchSession(
        sessionSummary: SessionSummary,
    ): Outcome

    suspend fun signOut()

    suspend fun resolveServer(handle: ProfileHandle): Result<Server>
}

@Inject
internal class AuthTokenRepository(
    @AppMainScope
    appMainScope: CoroutineScope,
    oauthRedirect: OauthRedirect,
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val profileDao: ProfileDao,
    private val networkService: NetworkService,
    private val identityResolver: IdentityResolver,
    private val savedStateDataSource: SavedStateDataSource,
    private val sessionManager: SessionManager,
    private val signedInUserRefresher: SignedInUserRefresher,
) : AuthRepository {

    init {
        appMainScope.launch {
            oauthRedirect.sessionRequests
                .collectLatest(::createSession)
        }
    }

    override val isSignedIn: Flow<Boolean> =
        savedStateDataSource.signedInAuth.map {
            it != null
        }
            .distinctUntilChanged()

    override val isGuest: Flow<Boolean> =
        savedStateDataSource.savedState.map { savedState ->
            savedState.auth is SavedState.AuthTokens.Guest
        }
            .distinctUntilChanged()

    override val signedInUser: Flow<Profile?> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            if (signedInProfileId == null) flowOf(null)
            else profileDao.profiles(
                signedInProfiledId = signedInProfileId.id,
                ids = listOf(signedInProfileId),
            )
                .distinctUntilChanged()
                .filter(List<PopulatedProfileEntity>::isNotEmpty)
                .map { it.first().asExternalModel() }
                .withRefresh(signedInUserRefresher::refresh)
        }
            .shareIn(
                scope = appMainScope + ioDispatcher,
                started = SharingStarted.WhileSubscribed(5_000),
                replay = 1,
            )

    override val pastSessions: Flow<List<SessionSummary>> =
        savedStateDataSource.savedState
            .map { it.pastSessions ?: emptyList() }
            .distinctUntilChanged()
            .shareIn(
                scope = appMainScope + ioDispatcher,
                started = SharingStarted.WhileSubscribed(5_000),
                replay = 1,
            )

    override fun isSignedInProfile(id: ProfileId): Flow<Boolean> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            flowOf(signedInProfileId == id)
        }

    override suspend fun oauthRequestUri(
        request: OauthUriRequest,
    ): Result<GenericUri> = runCatchingUnlessCancelled {
        when (val pendingToken = sessionManager.initiateOauthSession(request)) {
            is SavedState.AuthTokens.Pending.DPoP -> {
                savedStateDataSource.setAuth(pendingToken)
                // Suspend till auth token has been saved and is readable
                savedStateDataSource.savedState.first { it.auth == pendingToken }
                pendingToken.authorizeRequestUrl
                    .let(::GenericUri)
            }
        }
    }

    override suspend fun createSession(
        request: SessionRequest,
    ): Outcome = runCatchingUnlessCancelled {
        sessionManager.createSession(request)
    }
        .mapCatchingUnlessCancelled { authToken ->
            savedStateDataSource.setAuth(authToken)
            // Suspend till auth token has been saved and is readable
            savedStateDataSource.savedState.first { it.auth != null }

            // Check if it is an authenticated session. Guest sessions are valid.
            when (authToken) {
                is SavedState.AuthTokens.Authenticated ->
                    savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
                        if (authToken.authProfileId == signedInProfileId) signedInUserRefresher.refresh()
                        else expiredSessionOutcome()
                    }
                        ?: expiredSessionOutcome()
                else ->
                    Outcome.Success
            }
        }
        .fold(
            onSuccess = { it },
            onFailure = Outcome::Failure,
        )

    override suspend fun switchSession(
        sessionSummary: SessionSummary,
    ): Outcome = runCatchingUnlessCancelled {
        savedStateDataSource.inCurrentProfileSession { currentProfileId ->
            if (currentProfileId == sessionSummary.profileId) {
                return@inCurrentProfileSession
            }

            val freshAuth = savedStateDataSource.inPastSession(sessionSummary.profileId) {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getSession()
                }.getOrNull()

                val authenticatedToken = savedStateDataSource.savedState
                    .value
                    .profileData(sessionSummary.profileId)
                    ?.auth

                authenticatedToken as? SavedState.AuthTokens.Authenticated
            }
                ?: return@inCurrentProfileSession

            // Switching should cause the current session to expire
            savedStateDataSource.switchSession(
                profileId = sessionSummary.profileId,
                freshAuth = freshAuth,
            )

            logcat(LogPriority.WARN) {
                """
                    Session was successfully switched to ${sessionSummary.profileId},
                     this coroutine should have been cancelled.
                """.trimIndent()
            }
        }

        savedStateDataSource.inCurrentProfileSession { newProfileId ->
            when (newProfileId) {
                sessionSummary.profileId -> signedInUserRefresher.refresh()
                else -> throw SessionSwitchException(sessionSummary.profileHandle)
            }
        } ?: expiredSessionOutcome()
    }.toOutcome()

    override suspend fun signOut() {
        runCatchingUnlessCancelled {
            sessionManager.endSession()
        }
        savedStateDataSource.updateSignedInProfileData {
            // Clear any pending writes
            copy(writes = writes.copy(pendingWrites = emptyList()))
        }
        savedStateDataSource.setAuth(
            auth = null,
        )
    }

    override suspend fun resolveServer(
        handle: ProfileHandle,
    ): Result<Server> = runCatchingUnlessCancelled {
        identityResolver.resolveServer(Handle(handle.id))
            ?: throw IllegalStateException("Could not resolve server for handle: ${handle.id}")
    }
}
