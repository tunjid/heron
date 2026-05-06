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

package com.tunjid.heron.atmosphereapp

import androidx.lifecycle.ViewModel
import com.tunjid.heron.atmosphereapp.di.profileHandleOrId
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.models.RockSkyScrobble
import com.tunjid.heron.data.core.models.RockSkyTrack
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.records.RockSkyRecordOperations
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.timeline.state.recordStateHolder
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.atmosphereapp.generated.resources.Res
import heron.feature.atmosphereapp.generated.resources.tab_albums
import heron.feature.atmosphereapp.generated.resources.tab_artists
import heron.feature.atmosphereapp.generated.resources.tab_documents
import heron.feature.atmosphereapp.generated.resources.tab_publications
import heron.feature.atmosphereapp.generated.resources.tab_scrobbles
import heron.feature.atmosphereapp.generated.resources.tab_tracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualAtmosphereAppViewModel
}

@AssistedInject
class ActualAtmosphereAppViewModel(
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    rockSkyRecordOperations: RockSkyRecordOperations,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    AtmosphereAppStateHolder by scope.actionSuspendingStateMutator(
        initialState = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            profileLoadMutations(
                profileRepository,
                route,
                scope,
                state,
                recordRepository,
                rockSkyRecordOperations,
            )

            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.PageChanged -> action.flow.collect { event ->
                        state.currentPage = event.page
                    }
                    is Action.Navigate -> action.flow.collect { navAction ->
                        navActions(navAction.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun profileLoadMutations(
    profileRepository: ProfileRepository,
    route: Route,
    scope: CoroutineScope,
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
    rockSkyRecordOperations: RockSkyRecordOperations,
) {
    val sharedProfile = profileRepository.profile(route.profileHandleOrId)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )

    sharedProfile.launchAndCollect { profile ->
        state.profile = profile
    }

    sharedProfile
        .map { it.did }
        .distinctUntilChanged()
        .launchAndCollectLatest { resolvedProfileId ->
            val keysToHolders = state.stateHolders
                .associateBy(AppScreenStateHolders::key)

            state.stateHolders = stateHoldersFor(
                app = state.app,
                profileId = resolvedProfileId,
                existingHolders = keysToHolders,
                viewModelScope = scope,
                recordRepository = recordRepository,
                rockSkyRecordOperations = rockSkyRecordOperations,
            )
        }
}

private fun stateHoldersFor(
    app: AtmosphereApp?,
    profileId: ProfileId,
    existingHolders: Map<String, AppScreenStateHolders>,
    viewModelScope: CoroutineScope,
    recordRepository: RecordRepository,
    rockSkyRecordOperations: RockSkyRecordOperations,
): List<AppScreenStateHolders> = when (app?.id) {
    AtmosphereApp.StandardSiteId -> listOf(
        existingHolders[StandardDocumentUri.NAMESPACE]
            ?: AppScreenStateHolders.StandardSite.Documents(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_documents,
                    itemId = StandardDocument::uri,
                    cursorListLoader = recordRepository::authorDocuments,
                ),
            ),
        existingHolders[StandardPublicationUri.NAMESPACE]
            ?: AppScreenStateHolders.StandardSite.Publications(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_publications,
                    itemId = StandardPublication::uri,
                    cursorListLoader = recordRepository::authorPublications,
                ),
            ),
    )
    AtmosphereApp.RockskyId -> listOf(
        existingHolders[AlbumUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Albums(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_albums,
                    itemId = RockSkyAlbum::cid,
                    cursorListLoader = rockSkyRecordOperations::albums,
                ),
            ),
        existingHolders[TrackUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Tracks(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_tracks,
                    itemId = RockSkyTrack::cid,
                    cursorListLoader = rockSkyRecordOperations::tracks,
                ),
            ),
        existingHolders[ArtistUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Artists(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_artists,
                    itemId = RockSkyArtist::uri,
                    cursorListLoader = rockSkyRecordOperations::artists,
                ),
            ),
        existingHolders[ScrobbleUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Scrobbles(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_scrobbles,
                    itemId = RockSkyScrobble::cid,
                    cursorListLoader = rockSkyRecordOperations::scrobbles,
                ),
            ),
    )
    else -> emptyList()
}
