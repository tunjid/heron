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

import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.GetProfileQueryParams
import app.bsky.actor.SavedFeedType
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.graph.GetListQueryParams
import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.SessionSwitchException
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.SessionManager
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.preferenceupdater.NotificationPreferenceUpdater
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

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

    suspend fun updateSignedInUser(): Outcome
}

@Inject
internal class AuthTokenRepository(
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val preferenceUpdater: PreferenceUpdater,
    private val notificationPreferenceUpdater: NotificationPreferenceUpdater,
    private val savedStateDataSource: SavedStateDataSource,
    private val sessionManager: SessionManager,
) : AuthRepository {

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
                .withRefresh(::updateSignedInUser)
        }

    override val pastSessions: Flow<List<SessionSummary>> =
        savedStateDataSource.savedState
            .map { it.pastSessions ?: emptyList() }
            .distinctUntilChanged()

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
                        if (authToken.authProfileId == signedInProfileId) updateSignedInUser(
                            did = signedInProfileId.id.let(::Did),
                        )
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
                sessionSummary.profileId -> updateSignedInUser()
                else -> SessionSwitchException(sessionSummary.profileId)
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

    override suspend fun updateSignedInUser(): Outcome =
        savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
            if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

            networkService.runCatchingWithMonitoredNetworkRetry {
                getSession()
            }.fold(
                onSuccess = { updateSignedInUser(it.did) },
                onFailure = Outcome::Failure,
            )
        } ?: expiredSessionOutcome()

    private suspend fun updateSignedInUser(
        did: Did,
    ): Outcome = supervisorScope {
        val succeeded = listOf(
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getProfile(GetProfileQueryParams(actor = did))
                }
                    .getOrNull()
                    ?.profileEntity()
                    ?.let { profileEntity ->
                        profileDao.upsertProfiles(listOf(profileEntity))
                        savedStateDataSource.updateSignedInProfileData {
                            copy(
                                sessionSummary = SessionSummary(
                                    lastSeen = Clock.System.now(),
                                    profileId = profileEntity.did,
                                    profileHandle = profileEntity.handle,
                                    profileAvatar = profileEntity.avatar,
                                ),
                            )
                        }
                    } != null
            },
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getPreferencesForActor()
                }
                    .getOrNull()
                    ?.let { savePreferences(it) } != null
            },
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getPreferencesForNotification()
                }
                    .getOrNull()
                    ?.let { saveNotificationPreferences(it) } != null
            },
        ).awaitAll().all(true::equals)

        if (succeeded) Outcome.Success else Outcome.Failure(Exception("Unable to refresh user"))
    }

    private suspend fun savePreferences(
        preferencesResponse: GetPreferencesResponse,
    ) = supervisorScope {
        val preferences = preferenceUpdater.update(
            networkPreferences = preferencesResponse.preferences,
            preferences = savedStateDataSource.savedState
                .map(SavedState::signedProfilePreferencesOrDefault)
                .first(),
        )

        val saveTimelinePreferences = async {
            savedStateDataSource.updateSignedInProfileData {
                copy(preferences = preferences)
            }
        }
        val types = preferences.timelinePreferences.groupBy(
            keySelector = TimelinePreference::type,
        )

        val feeds = types[SavedFeedType.Feed.value]?.map {
            async {
                networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                    getFeedGenerator(
                        GetFeedGeneratorQueryParams(
                            feed = AtUri(it.value),
                        ),
                    )
                }
            }
        } ?: emptyList()
        val lists = types[SavedFeedType.List.value]?.map {
            async {
                networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                    getList(
                        GetListQueryParams(
                            cursor = null,
                            limit = 1,
                            list = AtUri(it.value),
                        ),
                    )
                }
            }
        } ?: emptyList()

        saveTimelinePreferences.await()
        multipleEntitySaverProvider.saveInTransaction {
            feeds.mapNotNull { it.await().getOrNull() }.forEach { add(it.view) }
            lists.mapNotNull { it.await().getOrNull() }.forEach { add(it.list) }
        }
    }

    private suspend fun saveNotificationPreferences(
        notificationPreferencesResponse: app.bsky.notification.GetPreferencesResponse,
    ) {
        val currentNotifications = savedStateDataSource.savedState
            .map { it.signedInProfileData?.notifications }
            .first() ?: SavedState.Notifications()

        val updatedNotifications = notificationPreferenceUpdater.update(
            notificationPreferences = notificationPreferencesResponse.preferences,
            notifications = currentNotifications,
        )

        savedStateDataSource.updateSignedInUserNotifications {
            copy(
                preferences = updatedNotifications.preferences,
                lastRefreshed = Clock.System.now(),
            )
        }
    }
}
