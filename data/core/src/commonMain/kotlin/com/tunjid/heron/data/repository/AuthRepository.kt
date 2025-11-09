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
import app.bsky.actor.Type
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.graph.GetListQueryParams
import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.SessionManager
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

interface AuthRepository {
    val isSignedIn: Flow<Boolean>

    val signedInUser: Flow<Profile?>

    fun isSignedInProfile(id: ProfileId): Flow<Boolean>

    suspend fun oauthRequestUri(
        request: OauthUriRequest,
    ): Result<GenericUri>

    suspend fun createSession(
        request: SessionRequest,
    ): Result<Unit>

    suspend fun signOut()

    suspend fun updateSignedInUser(): Outcome
}

@Inject
internal class AuthTokenRepository(
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val preferenceUpdater: PreferenceUpdater,
    private val savedStateDataSource: SavedStateDataSource,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> =
        savedStateDataSource.signedInAuth.map {
            it != null
        }

    override val signedInUser: Flow<Profile?> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val signedInUserFlow = signedInProfileId
                ?.let(::listOf)
                ?.let(profileDao::profiles)
                ?.filter(List<PopulatedProfileEntity>::isNotEmpty)
                ?.map { it.first().asExternalModel() }
                ?: flowOf(null)
            signedInUserFlow.withRefresh {
                if (signedInProfileId != null) updateSignedInUser()
            }
        }

    override fun isSignedInProfile(id: ProfileId): Flow<Boolean> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            flowOf(signedInProfileId == id)
        }

    override suspend fun oauthRequestUri(
        request: OauthUriRequest,
    ): Result<GenericUri> = runCatchingUnlessCancelled {
        sessionManager.startOauthSessionUri(request)
    }

    override suspend fun createSession(
        request: SessionRequest,
    ): Result<Unit> = runCatchingUnlessCancelled {
        sessionManager.createSession(request)
    }
        .mapCatchingUnlessCancelled { authToken ->
            savedStateDataSource.setAuth(authToken)
            // Suspend till auth token has been saved and is readable
            savedStateDataSource.savedState.first { it.auth != null }
            if (authToken is SavedState.AuthTokens.Authenticated) {
                updateSignedInUser(authToken.authProfileId.id.let(::Did))
            }
            Unit
        }
        .onFailure {
            savedStateDataSource.setAuth(null)
        }

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
        networkService.runCatchingWithMonitoredNetworkRetry {
            getSession()
        }.fold(
            onSuccess = { updateSignedInUser(it.did) },
            onFailure = Outcome::Failure,
        )

    private suspend fun updateSignedInUser(did: Did): Outcome = supervisorScope {
        val succeeded = listOf(
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getProfile(GetProfileQueryParams(actor = did))
                }
                    .getOrNull()
                    ?.profileEntity()
                    ?.let { profileDao.upsertProfiles(listOf(it)) } != null
            },
            async {
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getPreferences()
                }
                    .getOrNull()
                    ?.let { savePreferences(it) } != null
            },
        ).awaitAll().all(true::equals)

        if (succeeded) Outcome.Success else Outcome.Failure(Exception("Unable to refresh user"))
    }

    private suspend fun savePreferences(
        preferencesResponse: GetPreferencesResponse,
    ) = supervisorScope {
        val preferences = preferenceUpdater.update(
            response = preferencesResponse,
            preferences = savedStateDataSource.savedState
                .map { it.signedInProfileData?.preferences ?: Preferences.EmptyPreferences }
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

        val feeds = types[Type.Feed.value]?.map {
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
        val lists = types[Type.List.value]?.map {
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
}
