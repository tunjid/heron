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

package com.tunjid.heron.data.utilities.signedInUserRefresher

import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.GetProfileQueryParams
import app.bsky.actor.SavedFeedType
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.GetFeedGeneratorResponse
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetListResponse
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionOutcome
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.signedProfilePreferencesOrDefault
import com.tunjid.heron.data.repository.updateSignedInProfileData
import com.tunjid.heron.data.repository.updateSignedInUserNotifications
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.preferenceupdater.NotificationPreferenceUpdater
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

internal interface SignedInUserRefresher {
    suspend fun refresh(): Outcome
}

@Inject
internal class OfflineSignedInUserRefresher(
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val preferenceUpdater: PreferenceUpdater,
    private val notificationPreferenceUpdater: NotificationPreferenceUpdater,
    private val savedStateDataSource: SavedStateDataSource,
) : SignedInUserRefresher {

    override suspend fun refresh(): Outcome =
        savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
            if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

            networkService.runCatchingWithMonitoredNetworkRetry {
                getSession()
            }.fold(
                onSuccess = {
                    refresh(
                        did = signedInProfileId.id.let(::Did),
                    )
                },
                onFailure = Outcome::Failure,
            )
        } ?: expiredSessionOutcome()

    private suspend fun refresh(
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
                    getPreferencesForAppBskyActor()
                }
                    .getOrNull()
                    ?.let { savePreferences(it) } != null
            },
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getPreferencesForAppBskyNotification()
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

        // Await the network results before opening the DB transaction so the
        // writer connection is not held during I/O.
        val resolvedFeeds = feeds.awaitAll()
            .mapNotNull(Result<GetFeedGeneratorResponse>::getOrNull)
        val resolvedLists = lists.awaitAll()
            .mapNotNull(Result<GetListResponse>::getOrNull)

        multipleEntitySaverProvider.saveInTransaction {
            resolvedFeeds.forEach { add(it.view) }
            resolvedLists.forEach { add(it.list) }
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
