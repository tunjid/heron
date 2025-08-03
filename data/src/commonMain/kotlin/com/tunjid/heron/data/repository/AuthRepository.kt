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
import app.bsky.actor.PreferencesUnion
import app.bsky.actor.Type
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.graph.GetListQueryParams
import com.atproto.server.CreateSessionRequest
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

interface AuthRepository {
    val isSignedIn: Flow<Boolean>

    val signedInUser: Flow<Profile?>

    fun isSignedInProfile(id: Id): Flow<Boolean>

    suspend fun createSession(request: SessionRequest): Result<Unit>

    suspend fun guestSignIn()

    suspend fun signOut()

    suspend fun updateSignedInUser(): Boolean
}

@Inject
internal class AuthTokenRepository(
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> =
        savedStateDataSource.signedInAuth.map {
            it != null
        }

    override val signedInUser: Flow<Profile?> =
        savedStateDataSource.observedSignedInProfileId
            .flatMapLatest { signedInProfileId ->
                val signedInUserFlow = signedInProfileId
                    ?.let(::listOf)
                    ?.let(profileDao::profiles)
                    ?.filter(List<ProfileEntity>::isNotEmpty)
                    ?.map { it.first().asExternalModel() }
                    ?: flowOf(null)
                signedInUserFlow.withRefresh {
                    if (signedInProfileId != null) updateSignedInUser()
                }
            }

    override fun isSignedInProfile(id: Id): Flow<Boolean> =
        savedStateDataSource.savedState
            .map { it.signedInProfileId == id }
            .distinctUntilChanged()

    override suspend fun createSession(
        request: SessionRequest,
    ): Result<Unit> = networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
        createSession(
            CreateSessionRequest(
                identifier = request.username,
                password = request.password,
            )
        )
    }
        .mapCatching { result ->
            savedStateDataSource.updateState {
                copy(
                    auth = SavedState.AuthTokens(
                        authProfileId = ProfileId(result.did.did),
                        auth = result.accessJwt,
                        refresh = result.refreshJwt,
                        didDoc = SavedState.AuthTokens.DidDoc.fromJsonContentOrEmpty(
                            jsonContent = result.didDoc,
                        ),
                    )
                )
            }
            // Suspend till auth token has been saved and is readable
            savedStateDataSource.savedState.first { it.auth != null }
            updateSignedInUser(result.did)
        }

    override suspend fun guestSignIn() {
        savedStateDataSource.guestSignIn()
    }

    override suspend fun signOut() {
        savedStateDataSource.updateState {
            copy(auth = null)
        }
    }

    override suspend fun updateSignedInUser(): Boolean {
        return networkService.runCatchingWithMonitoredNetworkRetry {
            getSession()
        }
            .getOrNull()
            ?.did
            ?.let { updateSignedInUser(it) } == true
    }

    private suspend fun updateSignedInUser(did: Did) = supervisorScope {
        listOf(
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
    }

    private suspend fun savePreferences(
        preferencesResponse: GetPreferencesResponse,
    ) = supervisorScope {
        val preferences = preferencesResponse.toExternalModel()

        val saveTimelinePreferences = async {
            savedStateDataSource.updateSignedInUserPreferences(preferences)
        }
        val types = preferences.timelinePreferences.groupBy(
            keySelector = TimelinePreference::type,
        )

        val feeds = types[Type.Feed.value]?.map {
            async {
                networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                    getFeedGenerator(
                        GetFeedGeneratorQueryParams(
                            feed = AtUri(it.value)
                        )
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
                            list = AtUri(it.value)
                        )
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

private fun GetPreferencesResponse.toExternalModel() =
    preferences.fold(
        initial = Preferences.DefaultPreferences,
        operation = { preferences, preferencesUnion ->
            when (preferencesUnion) {
                is PreferencesUnion.AdultContentPref -> preferences
                is PreferencesUnion.BskyAppStatePref -> preferences
                is PreferencesUnion.ContentLabelPref -> preferences.copy(
                    contentLabelPreferences = preferences.contentLabelPreferences
                            + preferencesUnion.asExternalModel()
                )

                is PreferencesUnion.FeedViewPref -> preferences
                is PreferencesUnion.HiddenPostsPref -> preferences
                is PreferencesUnion.InterestsPref -> preferences
                is PreferencesUnion.LabelersPref -> preferences
                is PreferencesUnion.MutedWordsPref -> preferences
                is PreferencesUnion.PersonalDetailsPref -> preferences
                is PreferencesUnion.SavedFeedsPref -> preferences
                is PreferencesUnion.SavedFeedsPrefV2 -> preferences.copy(
                    timelinePreferences = preferencesUnion.value.items.map {
                        TimelinePreference(
                            id = it.id,
                            type = it.type.value,
                            value = it.value,
                            pinned = it.pinned,
                        )
                    }
                )

                is PreferencesUnion.ThreadViewPref -> preferences
                is PreferencesUnion.Unknown -> preferences
                is PreferencesUnion.PostInteractionSettingsPref -> preferences
                is PreferencesUnion.VerificationPrefs -> preferences
            }
        }
    )

private fun PreferencesUnion.ContentLabelPref.asExternalModel() = ContentLabelPreference(
    labelerId = value.labelerDid?.did?.let(::ProfileId),
    label = Label.Value(value = value.label),
    visibility = Label.Visibility(value = value.visibility.value),
)

