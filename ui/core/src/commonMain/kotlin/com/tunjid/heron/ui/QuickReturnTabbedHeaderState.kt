package com.tunjid.heron.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.AccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun QuickReturnTabbedLayout(
    modifier: Modifier = Modifier,
    header: @Composable QuickReturnTabbedHeaderState.() -> Unit,
    body: @Composable QuickReturnTabbedHeaderState.() -> Unit,
) {
    val topClearanceState = rememberUpdatedState(UiTokens.statusAndToolbarHeight)
    val tabsOffsetNestedScrollConnection = rememberAccumulatedOffsetNestedScrollConnection(
        maxOffset = { Offset.Zero },
        minOffset = { Offset(x = 0f, y = -UiTokens.toolbarHeight.toPx()) },
    )

    val state = remember(
        topClearanceState,
        tabsOffsetNestedScrollConnection,
    ) {
        AccumulatedOffsetQuickReturnTabbedHeaderState(
            statusAndToolbarHeight = topClearanceState::value,
            tabNestedScrollConnection = tabsOffsetNestedScrollConnection,
        )
    }

    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .zIndex(1f)
                .offset {
                    IntOffset(
                        x = 0,
                        y = topClearanceState.value.roundToPx(),
                    ) + tabsOffsetNestedScrollConnection.offset.round()
                },
        ) {
            state.header()
        }
        Box(
            modifier = Modifier
                .zIndex(0f)
                .nestedScroll(tabsOffsetNestedScrollConnection),
        ) {
            state.body()
        }
    }
}

@Composable
fun QuickReturnTabbedHeaderState.PagerTopGapCloseEffect(
    pagerState: PagerState,
    firstVisibleItemIndex: () -> Int,
    firstVisibleItemScrollOffset: () -> Int,
    scrollBy: suspend (Float) -> Unit,
) = when (this) {
    is AccumulatedOffsetQuickReturnTabbedHeaderState -> tabNestedScrollConnection.PagerTopGapCloseEffect(
        pagerState = pagerState,
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        scrollBy = scrollBy,
    )
}

@Composable
fun QuickReturnTabbedHeaderState.TabsCollapseHintEffect(
    onCollapseHint: (Boolean) -> Unit,
) = when (this) {
    is AccumulatedOffsetQuickReturnTabbedHeaderState -> LaunchedEffect(this) {
        snapshotFlow {
            tabNestedScrollConnection.verticalOffsetProgress() > 0.5f
        }
            .distinctUntilChanged()
            .collect { shouldCollapse ->
                onCollapseHint(shouldCollapse)
            }
    }
}

@Stable
sealed interface QuickReturnTabbedHeaderState {
    val topClearance: Dp
    val tabNestedScrollConnection: AccumulatedOffsetNestedScrollConnection
}

@Stable
private class AccumulatedOffsetQuickReturnTabbedHeaderState(
    private val statusAndToolbarHeight: () -> Dp,
    override val tabNestedScrollConnection: AccumulatedOffsetNestedScrollConnection,
) : QuickReturnTabbedHeaderState {
    override val topClearance: Dp
        get() = statusAndToolbarHeight()
}
