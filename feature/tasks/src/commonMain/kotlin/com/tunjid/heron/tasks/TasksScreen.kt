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

package com.tunjid.heron.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tasks.ui.FailedTaskCard
import com.tunjid.heron.tasks.ui.InFlightTaskCard
import com.tunjid.heron.tasks.ui.TaskItem
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.tabIndex
import heron.feature.tasks.generated.resources.Res
import heron.feature.tasks.generated.resources.empty_failed
import heron.feature.tasks.generated.resources.empty_failed_description
import heron.feature.tasks.generated.resources.empty_in_flight
import heron.feature.tasks.generated.resources.empty_in_flight_description
import heron.feature.tasks.generated.resources.failed
import heron.feature.tasks.generated.resources.in_flight
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TasksScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.initialPage,
    ) { 2 }
    val scope = rememberCoroutineScope()
    val topClearance = UiTokens.statusBarHeight + UiTokens.toolbarHeight

    val tabs = listOf(
        Tab(
            title = stringResource(Res.string.in_flight),
            hasUpdate = false,
        ),
        Tab(
            title = stringResource(Res.string.failed),
            hasUpdate = state.failed.isNotEmpty(),
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topClearance),
    ) {
        Tabs(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            tabsState = rememberTabsState(
                tabs = tabs,
                selectedTabIndex = pagerState::tabIndex,
                onTabSelected = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                },
                onTabReselected = {},
            ),
        )
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            userScrollEnabled = !paneScaffoldState.isTransitionActive,
        ) { page ->
            val contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                top = 8.dp,
                horizontal = 16.dp,
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            )
            when (page) {
                0 -> InFlightList(
                    inFlight = state.inFlight,
                    paneScaffoldState = paneScaffoldState,
                    contentPadding = contentPadding,
                )
                else -> FailedList(
                    failed = state.failed,
                    actions = actions,
                    paneScaffoldState = paneScaffoldState,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun InFlightList(
    inFlight: List<TaskItem.InFlight>,
    paneScaffoldState: PaneScaffoldState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    if (inFlight.isEmpty()) EmptyContent(
        modifier = Modifier.fillMaxSize(),
        titleRes = Res.string.empty_in_flight,
        descriptionRes = Res.string.empty_in_flight_description,
        icon = Icons.Rounded.TaskAlt,
    )
    else LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = inFlight,
            key = { it.writable.queueId },
            itemContent = { item ->
                InFlightTaskCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    item = item,
                    paneTransitionScope = paneScaffoldState,
                )
            },
        )
    }
}

@Composable
private fun FailedList(
    failed: List<TaskItem.Failed>,
    actions: (Action) -> Unit,
    paneScaffoldState: PaneScaffoldState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    if (failed.isEmpty()) EmptyContent(
        modifier = Modifier.fillMaxSize(),
        titleRes = Res.string.empty_failed,
        descriptionRes = Res.string.empty_failed_description,
        icon = Icons.Rounded.CloudDone,
    )
    else LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = failed,
            key = { it.failedWrite.writable.queueId },
            itemContent = { item ->
                FailedTaskCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    item = item,
                    paneTransitionScope = paneScaffoldState,
                    onRetry = { actions(Action.Retry(item.failedWrite)) },
                    onDismiss = { actions(Action.Dismiss(item.failedWrite)) },
                )
            },
        )
    }
}
