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

package com.tunjid.heron.timeline.ui.effects

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.domain.timeline.TimelineState
import com.tunjid.heron.domain.timeline.TimelineStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan

@Composable
fun <LazyState : ScrollableState> LazyState.TimelineRefreshEffect(
    timelineState: TimelineState,
    onRefresh: suspend LazyState.() -> Unit,
) {
    val updatedTimelineState by rememberUpdatedState(timelineState)
    LaunchedEffect(this) {
        snapshotFlow { updatedTimelineState.status }
            // Scan to make sure its not the first refreshed emission
            .scan(Pair<TimelineStatus?, TimelineStatus?>(null, null)) { pair, current ->
                pair.copy(first = pair.second, second = current)
            }
            .filter { (first, second) ->
                first != null && first != second && second is TimelineStatus.Refreshed
            }
            .collectLatest {
                onRefresh()
            }
    }
}
