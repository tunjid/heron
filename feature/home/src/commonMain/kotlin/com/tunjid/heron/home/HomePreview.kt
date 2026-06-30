/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.home.di.Route as HomeRoute
import com.tunjid.heron.timeline.state.preview.stubTimelineStateHolder
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf
import kotlin.time.Instant

@Preview
@Composable
internal fun HomePreview() {
    val scope = rememberCoroutineScope()
    val timeline = Timeline.Home.Following(
        name = "Following",
        position = 0,
        lastRefreshed = Instant.parse("2024-01-01T00:00:00Z"),
        itemsAvailable = 0,
        presentation = Timeline.Presentation.Text.WithEmbed,
        isPinned = true,
    )
    RoutePreview(
        route = routeOf(path = "/home"),
        routeStateHolder = remember(scope) {
            ActualHomeViewModel(
                mutator = State.Immutable(
                    currentTabUri = timeline.uri,
                    timelines = listOf(timeline),
                    timelineStateHolders = listOf(
                        HomeScreenStateHolders.Pinned(
                            mutator = stubTimelineStateHolder(
                                timeline = timeline,
                            ),
                        ),
                    ),
                ).asNoOpActionSuspendingStateMutator(),
                scope = scope,
            )
        },
        render = { route, paneScaffoldState ->
            HomeRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
