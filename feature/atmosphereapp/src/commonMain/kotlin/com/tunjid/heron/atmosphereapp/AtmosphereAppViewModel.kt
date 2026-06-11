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
import com.tunjid.heron.data.core.models.DerakkumaBest
import com.tunjid.heron.data.core.models.DerakkumaCircle
import com.tunjid.heron.data.core.models.DerakkumaCircleMember
import com.tunjid.heron.data.core.models.DerakkumaFavoriteSong
import com.tunjid.heron.data.core.models.DerakkumaFriend
import com.tunjid.heron.data.core.models.DerakkumaPlay
import com.tunjid.heron.data.core.models.DerakkumaProfile
import com.tunjid.heron.data.core.models.RockskyAlbum
import com.tunjid.heron.data.core.models.RockskyArtist
import com.tunjid.heron.data.core.models.RockskyScrobble
import com.tunjid.heron.data.core.models.RockskyTrack
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.DerakkumaBestUri
import com.tunjid.heron.data.core.types.DerakkumaCircleMemberUri
import com.tunjid.heron.data.core.types.DerakkumaCircleUri
import com.tunjid.heron.data.core.types.DerakkumaFavoriteSongUri
import com.tunjid.heron.data.core.types.DerakkumaFriendUri
import com.tunjid.heron.data.core.types.DerakkumaPlayUri
import com.tunjid.heron.data.core.types.DerakkumaProfileUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.timeline.state.recordStateHolder
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.mutableTiledListOf
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.atmosphereapp.generated.resources.Res
import heron.feature.atmosphereapp.generated.resources.tab_albums
import heron.feature.atmosphereapp.generated.resources.tab_artists
import heron.feature.atmosphereapp.generated.resources.tab_bests
import heron.feature.atmosphereapp.generated.resources.tab_circle
import heron.feature.atmosphereapp.generated.resources.tab_documents
import heron.feature.atmosphereapp.generated.resources.tab_favorites
import heron.feature.atmosphereapp.generated.resources.tab_friends
import heron.feature.atmosphereapp.generated.resources.tab_members
import heron.feature.atmosphereapp.generated.resources.tab_plays
import heron.feature.atmosphereapp.generated.resources.tab_profile
import heron.feature.atmosphereapp.generated.resources.tab_publications
import heron.feature.atmosphereapp.generated.resources.tab_scrobbles
import heron.feature.atmosphereapp.generated.resources.tab_tracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    AtmosphereAppStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchProfileLoadMutations(
                profileRepository,
                route,
                scope,
                state,
                recordRepository,
            )

            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.PageChanged -> action.flow.collect { event ->
                        state.currentPage = event.page
                    }
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )
                    is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.Navigate -> action.flow.collect { navAction ->
                        navActions(navAction.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchProfileLoadMutations(
    profileRepository: ProfileRepository,
    route: Route,
    scope: CoroutineScope,
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
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
            )
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        when (action) {
            is Action.TogglePublicationSubscription.Subscribe -> Writable.StandardSite.Subscribe(
                create = StandardSubscription.Create(publicationUri = action.publicationUri),
            )
            is Action.TogglePublicationSubscription.Unsubscribe -> Writable.RecordDeletion(
                recordUri = action.subscriptionUri,
            )
        }
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun stateHoldersFor(
    app: AtmosphereApp?,
    profileId: ProfileId,
    existingHolders: Map<String, AppScreenStateHolders>,
    viewModelScope: CoroutineScope,
    recordRepository: RecordRepository,
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
        existingHolders[ScrobbleUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Scrobbles(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_scrobbles,
                    itemId = RockskyScrobble::uri,
                    cursorListLoader = recordRepository::scrobbles,
                ),
            ),
        existingHolders[ArtistUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Artists(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_artists,
                    itemId = RockskyArtist::uri,
                    cursorListLoader = recordRepository::artists,
                ),
            ),
        existingHolders[TrackUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Tracks(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_tracks,
                    itemId = RockskyTrack::uri,
                    cursorListLoader = recordRepository::tracks,
                ),
            ),
        existingHolders[AlbumUri.NAMESPACE]
            ?: AppScreenStateHolders.Rocksky.Albums(
                mutator = viewModelScope.recordStateHolder(
                    profileId = profileId,
                    stringResource = Res.string.tab_albums,
                    itemId = RockskyAlbum::uri,
                    cursorListLoader = recordRepository::albums,
                ),
            ),
    )
    AtmosphereApp.DerakkumaId -> listOf(
        existingHolders[DerakkumaProfileUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.Profiles(viewModelScope.recordStateHolder(profileId, Res.string.tab_profile, DerakkumaProfile::uri, recordRepository::derakkumaProfiles)),
        existingHolders[DerakkumaPlayUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.Plays(viewModelScope.derakkumaPlayStateHolder(profileId, recordRepository)),
        existingHolders[DerakkumaBestUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.Bests(viewModelScope.recordStateHolder(profileId, Res.string.tab_bests, DerakkumaBest::uri, recordRepository::derakkumaBests)),
        existingHolders[DerakkumaFriendUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.Friends(viewModelScope.recordStateHolder(profileId, Res.string.tab_friends, DerakkumaFriend::uri, recordRepository::derakkumaFriends)),
        existingHolders[DerakkumaFavoriteSongUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.FavoriteSongs(viewModelScope.recordStateHolder(profileId, Res.string.tab_favorites, DerakkumaFavoriteSong::uri, recordRepository::derakkumaFavoriteSongs)),
        existingHolders[DerakkumaCircleUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.Circle(viewModelScope.recordStateHolder(profileId, Res.string.tab_circle, DerakkumaCircle::uri, recordRepository::derakkumaCircle)),
        existingHolders[DerakkumaCircleMemberUri.NAMESPACE] ?: AppScreenStateHolders.Derakkuma.CircleMembers(viewModelScope.recordStateHolder(profileId, Res.string.tab_members, DerakkumaCircleMember::uri, recordRepository::derakkumaCircleMembers)),
    )
    else -> emptyList()
}

private fun CoroutineScope.derakkumaPlayStateHolder(
    profileId: ProfileId,
    recordRepository: RecordRepository,
) = actionSuspendingStateMutator(
    state = com.tunjid.heron.timeline.state.RecordState.SnapshotMutable<DerakkumaPlay>(
        stringResource = Res.string.tab_plays,
        tilingData = com.tunjid.heron.tiling.TilingState.Data(
            currentQuery = com.tunjid.heron.data.repository.ProfilesQuery(
                profileId = profileId,
                data = com.tunjid.heron.data.core.models.CursorQuery.defaultStartData(),
            ),
        ),
    ),
    producer = { state, actions ->
        actions.launchTilingMutations(
            state = state,
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = recordRepository::derakkumaPlays,
            onNewItems = { items ->
                items
                    .distinctBy(DerakkumaPlay::uri)
                    .sortedDerakkumaPlays()
            },
        )
    },
)

private fun TiledList<com.tunjid.heron.data.repository.ProfilesQuery, DerakkumaPlay>.sortedDerakkumaPlays(): TiledList<com.tunjid.heron.data.repository.ProfilesQuery, DerakkumaPlay> {
    if (size < 2) return this
    val comparator = compareByDescending<DerakkumaPlay> { it.playedAt.derakkumaDateSortKey() }
        .thenByDescending { it.createdAt.derakkumaDateSortKey() }
        .thenByDescending { it.uri.uri }
    val sorted = indices
        .map { index -> queryAt(index) to get(index) }
        .sortedWith { first, second -> comparator.compare(first.second, second.second) }
    return mutableTiledListOf<com.tunjid.heron.data.repository.ProfilesQuery, DerakkumaPlay>().also { output ->
        sorted.forEach { (query, item) -> output.add(query, item) }
    }
}

private fun String.derakkumaDateSortKey(): Long = derakkumaDateRegex
    .find(this)
    ?.groupValues
    ?.drop(1)
    ?.map(String::toLongOrNull)
    ?.takeIf { parts -> parts.size == 5 && parts.all { it != null } }
    ?.let { parts ->
        val (year, month, day, hour, minute) = parts.map { it ?: 0 }
        year * 100_000_000L + month * 1_000_000L + day * 10_000L + hour * 100L + minute
    } ?: 0L

private val derakkumaDateRegex = Regex("""(\d{4})\D+(\d{1,2})\D+(\d{1,2})\D+(\d{1,2})\D+(\d{1,2})""")
