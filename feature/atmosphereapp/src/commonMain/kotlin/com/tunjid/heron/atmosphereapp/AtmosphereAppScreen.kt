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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.heron.atmosphereapp.ui.AtmosphereAppHeader
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.record.RecordList
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.ui.UiTokens

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

    val pagerState = rememberPagerState { state.stateHolders.size }
    val pullToRefreshState = rememberPullToRefreshState()

    val isRefreshing = state.stateHolders
        .getOrNull(pagerState.currentPage)
        .isRefreshing

    CollapsingHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = {
                    state.stateHolders
                        .getOrNull(pagerState.currentPage)
                        ?.refresh()
                },
            ),
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
                stateHolders = state.stateHolders,
            )
        },
        body = {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize(),
                state = pagerState,
                key = { page -> state.stateHolders[page].key },
                pageContent = { page ->
                    when (val stateHolder = state.stateHolders[page]) {
                        is AppScreenStateHolders.StandardSite.Documents -> RecordList(
                            collectionStateHolder = stateHolder,
                            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                            itemKey = { it.uri.uri },
                            itemContent = { document ->
                                Document(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .animateItem(),
                                    paneTransitionScope = paneScaffoldState,
                                    sharedElementPrefix = document.uri.uri,
                                    document = document,
                                    onPublicationClicked = null,
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
                                        .animateItem(),
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
                            itemKey = { it.cid.id },
                            itemContent = { album ->
                                RockskyRecordRow(
                                    title = album.title,
                                    subtitle = album.artist,
                                )
                            },
                        )
                        is AppScreenStateHolders.Rocksky.Tracks -> RecordList(
                            collectionStateHolder = stateHolder,
                            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                            itemKey = { it.cid.id },
                            itemContent = { track ->
                                RockskyRecordRow(
                                    title = track.title,
                                    subtitle = track.artist,
                                )
                            },
                        )
                        is AppScreenStateHolders.Rocksky.Artists -> RecordList(
                            collectionStateHolder = stateHolder,
                            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                            itemKey = { it.cid.id },
                            itemContent = { artist -> RockskyRecordRow(title = artist.name, subtitle = null) },
                        )
                        is AppScreenStateHolders.Rocksky.Scrobbles -> RecordList(
                            collectionStateHolder = stateHolder,
                            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                            itemKey = { it.cid.id },
                            itemContent = { scrobble ->
                                RockskyRecordRow(
                                    title = scrobble.title,
                                    subtitle = scrobble.artist,
                                )
                            },
                        )
                    }
                },
            )
        },
    )
    LaunchedEffect(Unit) {
        snapshotFlow {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).fastRoundToInt()
        }.collect { page ->
            actions(Action.PageChanged(page))
        }
    }
}

@Composable
private fun RockskyRecordRow(
    title: String,
    subtitle: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}
