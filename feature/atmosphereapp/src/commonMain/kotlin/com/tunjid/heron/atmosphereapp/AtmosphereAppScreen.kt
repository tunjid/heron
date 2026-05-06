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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.timeline.ui.record.RecordList
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import heron.feature.atmosphereapp.generated.resources.Res
import heron.feature.atmosphereapp.generated.resources.app_name_rocksky
import heron.feature.atmosphereapp.generated.resources.app_name_standard_site
import heron.feature.atmosphereapp.generated.resources.app_name_unknown
import heron.feature.atmosphereapp.generated.resources.tab_albums
import heron.feature.atmosphereapp.generated.resources.tab_artists
import heron.feature.atmosphereapp.generated.resources.tab_documents
import heron.feature.atmosphereapp.generated.resources.tab_publications
import heron.feature.atmosphereapp.generated.resources.tab_scrobbles
import heron.feature.atmosphereapp.generated.resources.tab_tracks
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AtmosphereAppScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { state.stateHolders.size }
    val pullToRefreshState = rememberPullToRefreshState()

    val isRefreshing = state.stateHolders
        .getOrNull(pagerState.currentPage)
        .isRefreshing

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize()
            .paneClip(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            state.stateHolders
                .getOrNull(pagerState.currentPage)
                ?.refresh()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            AtmosphereAppHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                profile = state.profile,
                app = state.app,
            )
            AtmosphereAppTabs(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                tabs = state.stateHolders.map { holder ->
                    Tab(
                        title = holder.tabTitle(),
                        id = holder.key,
                        hasUpdate = false,
                    )
                },
                pagerState = pagerState,
            )
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize(),
                state = pagerState,
                key = { page -> state.stateHolders[page].key },
                pageContent = { page ->
                    AtmosphereAppPage(
                        stateHolder = state.stateHolders[page],
                        paneScaffoldState = paneScaffoldState,
                    )
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).fastRoundToInt()
        }.collect { page ->
            actions(Action.PageChanged(page))
        }
    }
}

@Composable
private fun AtmosphereAppHeader(
    modifier: Modifier = Modifier,
    profile: Profile?,
    app: AtmosphereApp?,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverlappingAvatars(
            profile = profile,
            app = app,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "${profile?.displayName ?: profile?.handle?.id}'s ${app.displayName()}",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun OverlappingAvatars(
    profile: Profile?,
    app: AtmosphereApp?,
) {
    val outerSize = AvatarSize + AvatarOffset
    val appDisplayName = app.displayName()
    Box(
        modifier = Modifier
            .size(
                width = outerSize,
                height = outerSize,
            ),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(AvatarSize)
                .align(Alignment.TopStart)
                .clip(CircleShape),
            args = remember(profile?.avatar) {
                ImageArgs(
                    url = profile?.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile?.displayName ?: profile?.handle?.id,
                    shape = RoundedPolygonShape.Circle,
                )
            },
        )
        AsyncImage(
            modifier = Modifier
                .size(AvatarSize)
                .align(Alignment.BottomEnd)
                .clip(CircleShape),
            args = remember(
                app?.id,
                appDisplayName,
            ) {
                ImageArgs(
                    url = app?.logo
                        ?.uri
                        ?.takeIf(String::isNotBlank),
                    contentScale = ContentScale.Crop,
                    contentDescription = appDisplayName,
                    shape = RoundedPolygonShape.Circle,
                )
            },
        )
    }
}

@Composable
private fun AtmosphereAppTabs(
    modifier: Modifier = Modifier,
    tabs: List<Tab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    val scope = rememberCoroutineScope()
    Tabs(
        modifier = modifier.clip(CircleShape),
        tabsState = rememberTabsState(
            tabs = tabs,
            selectedTabIndex = pagerState::tabIndex,
            onTabSelected = {
                scope.launch { pagerState.animateScrollToPage(it) }
            },
            onTabReselected = {},
        ),
    )
}

@Composable
private fun AtmosphereAppPage(
    stateHolder: AppScreenStateHolders,
    paneScaffoldState: PaneScaffoldState,
) {
    when (stateHolder) {
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
                    onSubscriptionToggled = null,
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
                    onSubscriptionToggled = { _, _ -> },
                )
            },
        )
        is AppScreenStateHolders.Rocksky.Albums -> RecordList(
            collectionStateHolder = stateHolder,
            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
            itemKey = { it.cid.id },
            itemContent = { album -> RockskyRecordRow(title = album.title, subtitle = album.artist) },
        )
        is AppScreenStateHolders.Rocksky.Tracks -> RecordList(
            collectionStateHolder = stateHolder,
            prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
            itemKey = { it.cid.id },
            itemContent = { track -> RockskyRecordRow(title = track.title, subtitle = track.artist) },
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
            itemContent = { scrobble -> RockskyRecordRow(title = scrobble.title, subtitle = scrobble.artist) },
        )
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

@Composable
private fun AppScreenStateHolders.tabTitle(): String = stringResource(
    when (this) {
        is AppScreenStateHolders.StandardSite.Documents -> Res.string.tab_documents
        is AppScreenStateHolders.StandardSite.Publications -> Res.string.tab_publications
        is AppScreenStateHolders.Rocksky.Albums -> Res.string.tab_albums
        is AppScreenStateHolders.Rocksky.Tracks -> Res.string.tab_tracks
        is AppScreenStateHolders.Rocksky.Artists -> Res.string.tab_artists
        is AppScreenStateHolders.Rocksky.Scrobbles -> Res.string.tab_scrobbles
    },
)

@Composable
private fun AtmosphereApp?.displayName(): String = stringResource(
    when (this?.id) {
        AtmosphereApp.StandardSiteId -> Res.string.app_name_standard_site
        AtmosphereApp.RockskyId -> Res.string.app_name_rocksky
        else -> Res.string.app_name_unknown
    },
)

private val AvatarSize = 56.dp
private val AvatarOffset = 24.dp
