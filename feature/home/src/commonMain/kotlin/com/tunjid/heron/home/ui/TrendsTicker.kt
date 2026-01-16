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

package com.tunjid.heron.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Trend
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun TrendsTicker(
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    trends: List<Trend>,
    onTrendClicked: (Trend) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        if (trends.isNotEmpty()) ElevatedCard(
            modifier = Modifier
                .height(46.dp),
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var isExpanded by rememberSaveable { mutableStateOf(false) }
                var ticker by rememberSaveable { mutableStateOf(0) }
                val focusedIndex = ticker % trends.size
                val trend = trends[focusedIndex]

                AnimatedContent(
                    targetState = isExpanded,
                ) { currentlyExpanded ->
                    if (currentlyExpanded) HorizontalTicker(
                        modifier = Modifier
                            .padding(horizontal = 8.dp),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@AnimatedContent,
                        focusedIndex = focusedIndex,
                        trends = trends,
                        onCollapsed = { firstCompletelyVisibleIndex ->
                            ticker = firstCompletelyVisibleIndex
                            isExpanded = false
                        },
                        onTrendClicked = onTrendClicked,
                    ) else VerticalTicker(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@AnimatedContent,
                        trend = trend,
                        onClick = {
                            isExpanded = true
                        },
                    )
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { !isExpanded }
                        .collectLatest { collapsed ->
                            if (collapsed) while (isActive) {
                                delay(VerticalTickerChangeDelay)
                                ++ticker
                            }
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VerticalTicker(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    trend: Trend,
    onClick: () -> Unit,
) = with(sharedTransitionScope) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier,
            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        AnimatedContent(
            modifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = trend.link,
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            targetState = trend.displayName ?: "",
            transitionSpec = { TextCheckedTransform },
        ) { currentText ->
            Text(
                modifier = Modifier,
                text = currentText,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMediumEmphasized,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HorizontalTicker(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    focusedIndex: Int,
    trends: List<Trend>,
    onCollapsed: (Int) -> Unit,
    onTrendClicked: (Trend) -> Unit,
) = with(sharedTransitionScope) {
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = focusedIndex,
    )
    LazyRow(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onCollapsed(state.middleItemIndex) },
                    onVerticalDrag = { _, _ -> },
                )
            }
            .clickable { onCollapsed(state.middleItemIndex) },
        state = state,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = trends,
            key = Trend::link,
            itemContent = { trend ->
                trend.displayName?.let {
                    Text(
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(
                                    key = trend.link,
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                            .padding(
                                vertical = 4.dp,
                                horizontal = 8.dp,
                            )
                            .clickable { onTrendClicked(trend) },
                        text = it,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                    )
                }
            },
        )
    }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) return@LaunchedEffect
        launch {
            // Wait till scrolled to the end
            snapshotFlow { state.canScrollForward }
                .first(false::equals)
            delay(HorizontalTickerDismissDelay)
            onCollapsed(state.middleItemIndex)
        }

        while (isActive) {
            withFrameNanos {}
            state.scrollBy(CHYRON_SCROLL_DELTA)
        }
    }
}

private val LazyListState.middleItemIndex: Int
    get() {
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        val middleItem = visibleItemsInfo[visibleItemsInfo.size / 2]
        return middleItem.index
    }

private const val CHYRON_SCROLL_DELTA = 0.5f
private const val BUTTON_ANIMATION_DURATION_MILLIS = 600

private val TextAnimationSpec = tween<IntOffset>(BUTTON_ANIMATION_DURATION_MILLIS)

private val VerticalTickerChangeDelay = 4.seconds
private val HorizontalTickerDismissDelay = 3.seconds

private val TextCheckedTransform = slideInVertically(
    animationSpec = TextAnimationSpec,
    initialOffsetY = { it },
) togetherWith slideOutVertically(
    animationSpec = TextAnimationSpec,
    targetOffsetY = { -it },
)

