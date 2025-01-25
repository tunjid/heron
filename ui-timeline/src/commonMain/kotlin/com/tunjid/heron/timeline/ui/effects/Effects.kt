package com.tunjid.heron.timeline.ui.effects

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.domain.timeline.TimelineState
import com.tunjid.heron.domain.timeline.TimelineStatus
import com.tunjid.heron.media.video.LocalVideoPlayerController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
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

@Composable
fun PagerState.PauseVideoOnTabChangeEffect() {
    val videoPlayerController = LocalVideoPlayerController.current
    LaunchedEffect(this) {
        snapshotFlow { currentPage }
            .drop(1)
            .collect {
                videoPlayerController.pauseActiveVideo()
            }
    }
}