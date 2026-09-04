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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.heron.atmosphereapp.ui.AtmosphereAppHeader
import com.tunjid.heron.data.core.models.link
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.types.takeIfIs
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaBest
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaCircle
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaCircleMember
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaFavoriteSong
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaFriend
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaPlay
import com.tunjid.heron.timeline.ui.derakkuma.DerakkumaProfile
import com.tunjid.heron.timeline.ui.record.RecordList
import com.tunjid.heron.timeline.ui.rocksky.RockskyAlbum
import com.tunjid.heron.timeline.ui.rocksky.RockskyArtist
import com.tunjid.heron.timeline.ui.rocksky.RockskyScrobble
import com.tunjid.heron.timeline.ui.rocksky.RockskyTrack
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.standardPublicationDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.mutator.compose.produceStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtmosphereAppScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val collapsedHeight = with(density) {
        UiTokens.tabsHeight.toPx()
    }
    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = collapsedHeight,
        initialExpandedHeight = with(density) { 800.dp.toPx() },
    )

    val visibleStateHolders = remember(state.stateHolders) {
        state.stateHolders.filterNot { it is AppScreenStateHolders.Derakkuma.CircleMembers }
    }
    val pagerState = rememberPagerState { visibleStateHolders.size }
    val pullToRefreshState = rememberPullToRefreshState()
    val circleMembers = state.stateHolders
        .filterIsInstance<AppScreenStateHolders.Derakkuma.CircleMembers>()
        .firstOrNull()
        ?.produceStateWithLifecycle()
        ?.tiledItems
        .orEmpty()

    val isRefreshing = visibleStateHolders
        .getOrNull(pagerState.currentPage)
        .isRefreshing

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            visibleStateHolders
                .getOrNull(pagerState.currentPage)
                ?.refresh()
        },
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = collapsingHeaderState.expandedHeight.roundToInt(),
                        )
                    },
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
            )
        },
    ) {
        CollapsingHeaderLayout(
            modifier = Modifier
                .fillMaxSize(),
            state = collapsingHeaderState,
            headerContent = {
                AtmosphereAppHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    paneScaffoldState = paneScaffoldState,
                    headerState = collapsingHeaderState,
                    pagerState = pagerState,
                    avatarSharedElementKey = state.avatarSharedElementKey,
                    profile = state.profile,
                    app = state.app,
                    stateHolders = visibleStateHolders,
                )
            },
            body = {
                val uriHandler = LocalUriHandler.current
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = pagerState,
                    key = { page -> visibleStateHolders[page].key },
                    pageContent = { page ->
                        when (val stateHolder = visibleStateHolders[page]) {
                            is AppScreenStateHolders.StandardSite.Documents -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { document ->
                                    Document(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .shapedClickable {
                                                runCatching {
                                                    document.link
                                                        ?.takeIfIs(Uri.Host.Https)
                                                        ?.let(uriHandler::openUri)
                                                }
                                            }
                                            .padding(8.dp),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = document.uri.uri,
                                        document = document,
                                        onPublicationClicked = {
                                            actions(
                                                Action.Navigate.To(
                                                    standardPublicationDestination(
                                                        publication = it,
                                                        sharedElementPrefix = document.uri.uri,
                                                    ),
                                                ),
                                            )
                                        },
                                        onSubscriptionToggled = { publication, subscription ->
                                            actions(
                                                if (subscription != null) Action.TogglePublicationSubscription.Unsubscribe(
                                                    subscriptionUri = subscription.uri,
                                                )
                                                else Action.TogglePublicationSubscription.Subscribe(
                                                    publicationUri = publication.uri,
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                            is AppScreenStateHolders.StandardSite.Publications -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { publication ->
                                    Publication(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .animateItem()
                                            .shapedClickable {
                                                actions(
                                                    Action.Navigate.To(
                                                        pathDestination(
                                                            path = publication.uri.path,
                                                            models = listOf(publication),
                                                            sharedElementPrefix = publication.uri.uri,
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        ),
                                                    ),
                                                )
                                            },
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = publication.uri.uri,
                                        publication = publication,
                                        onSubscriptionToggled = { publication, subscription ->
                                            actions(
                                                if (subscription != null) Action.TogglePublicationSubscription.Unsubscribe(
                                                    subscriptionUri = subscription.uri,
                                                )
                                                else Action.TogglePublicationSubscription.Subscribe(
                                                    publicationUri = publication.uri,
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                            is AppScreenStateHolders.Rocksky.Albums -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { album ->
                                    RockskyAlbum(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shapedClickable {
                                                uriHandler.openRockskyLink(
                                                    recordUri = album.uri,
                                                    collection = "album",
                                                )
                                            }
                                            .padding(8.dp)
                                            .animateItem(),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = AtmosphereScreenSharedElementPrefix,
                                        album = album,
                                        onMusicServiceLinkClicked = { url ->
                                            runCatching {
                                                uriHandler.openUri(url)
                                            }
                                        },
                                    )
                                },
                            )
                            is AppScreenStateHolders.Rocksky.Tracks -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { track ->
                                    RockskyTrack(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shapedClickable {
                                                uriHandler.openRockskyLink(
                                                    recordUri = track.uri,
                                                    collection = "song",
                                                )
                                            }
                                            .padding(8.dp)
                                            .animateItem(),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = AtmosphereScreenSharedElementPrefix,
                                        track = track,
                                    )
                                },
                            )
                            is AppScreenStateHolders.Rocksky.Artists -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { artist ->
                                    RockskyArtist(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shapedClickable {
                                                uriHandler.openRockskyLink(
                                                    recordUri = artist.uri,
                                                    collection = "artist",
                                                )
                                            }
                                            .padding(8.dp)
                                            .animateItem(),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = AtmosphereScreenSharedElementPrefix,
                                        artist = artist,
                                    )
                                },
                            )
                            is AppScreenStateHolders.Rocksky.Scrobbles -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { scrobble ->
                                    RockskyScrobble(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shapedClickable {
                                                uriHandler.openRockskyLink(
                                                    recordUri = scrobble.uri,
                                                    collection = "scrobble",
                                                )
                                            }
                                            .padding(8.dp)
                                            .animateItem(),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = AtmosphereScreenSharedElementPrefix,
                                        scrobble = scrobble,
                                    )
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.Profiles -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { profile ->
                                    DerakkumaProfile(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, profile)
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.Plays -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { play ->
                                    DerakkumaPlay(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, play)
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.Bests -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { best ->
                                    DerakkumaBest(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, best)
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.Friends -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { friend ->
                                    DerakkumaFriend(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp).animateItem(),
                                        paneTransitionScope = paneScaffoldState,
                                        sharedElementPrefix = AtmosphereScreenSharedElementPrefix,
                                        friend = friend,
                                        onProfileClick = { profile, avatarSharedElementKey ->
                                            actions(
                                                Action.Navigate.To(
                                                    profileDestination(
                                                        profile = profile,
                                                        avatarSharedElementKey = avatarSharedElementKey,
                                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                    ),
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.FavoriteSongs -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { favorite ->
                                    DerakkumaFavoriteSong(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, favorite)
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.Circle -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { circle ->
                                    DerakkumaCircle(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, circle)
                                },
                                additionalContent = {
                                    items(
                                        items = circleMembers,
                                        key = { it.uri.uri },
                                    ) { member ->
                                        DerakkumaCircleMember(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, member)
                                    }
                                },
                            )
                            is AppScreenStateHolders.Derakkuma.CircleMembers -> RecordList(
                                collectionStateHolder = stateHolder,
                                prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                itemKey = { it.uri.uri },
                                itemContent = { member ->
                                    DerakkumaCircleMember(Modifier.fillMaxWidth().padding(8.dp).animateItem(), paneScaffoldState, AtmosphereScreenSharedElementPrefix, member)
                                },
                            )
                        }
                    },
                )
            },
        )
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).fastRoundToInt()
        }.collect { page ->
            actions(Action.PageChanged(page))
        }
    }
}

private fun UriHandler.openRockskyLink(
    recordUri: RecordUri,
    collection: String,
) {
    runCatching {
        openUri(
            "https://rocksky.app/${recordUri.profileId().id}/$collection/${recordUri.recordKey.value}",
        )
    }
}

private const val AtmosphereScreenSharedElementPrefix = "AtmosphereScreenSharedElementPrefix"
