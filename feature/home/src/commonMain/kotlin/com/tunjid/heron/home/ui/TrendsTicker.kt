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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.ui.AnimatedVerticallySlidingContent
import com.tunjid.heron.ui.AppBarTextButton
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.EmphasizedSingleLineOutlinedText
import heron.ui.core.generated.resources.close
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource

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
        var isVertical by rememberSaveable { mutableStateOf(true) }
        if (trends.isNotEmpty()) AppBarTextButton(
            onClick = { isVertical = !isVertical },
        ) {
            var ticker by rememberSaveable { mutableStateOf(0) }
            val focusedIndex = ticker % trends.size
            val trend = trends[focusedIndex]

            AnimatedContent(
                targetState = isVertical,
            ) { vertical ->
                if (vertical) VerticalTicker(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent,
                    trend = trend,
                )
                else HorizontalTicker(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent,
                    focusedIndex = focusedIndex,
                    trends = trends,
                    onCollapsed = { firstCompletelyVisibleIndex ->
                        ticker = firstCompletelyVisibleIndex
                        isVertical = true
                    },
                    onTrendClicked = onTrendClicked,
                )
            }

            LaunchedEffect(Unit) {
                snapshotFlow { isVertical }
                    .collectLatest { vertical ->
                        if (vertical) while (isActive) {
                            delay(VerticalTickerChangeDelay)
                            ++ticker
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
) = with(sharedTransitionScope) {
    Row(
        modifier = modifier
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier,
            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        AnimatedVerticallySlidingContent(
            modifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = trend.link,
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            targetState = trend.tickerValue,
        ) { currentText ->
            EmphasizedSingleLineOutlinedText(
                text = currentText,
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
    Row(
        modifier = modifier
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = trends,
                key = Trend::link,
                itemContent = { trend ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onTrendClicked(trend) }
                            .padding(
                                vertical = 4.dp,
                                horizontal = 6.dp,
                            )
                            .animateItem(),
                    ) {
                        EmphasizedSingleLineOutlinedText(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(
                                        key = trend.link,
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                ),
                            text = trend.tickerValue,
                        )
                    }
                },
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onCollapsed(state.middleItemIndex) }
                .padding(all = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Cancel,
                contentDescription = stringResource(CommonStrings.close),
            )
        }
    }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) return@LaunchedEffect

        while (isActive) {
            with(state) {
                // When scrolling backward, continue until the start, then reverse.
                // Otherwise, scroll forward until the end, then reverse.
                val shouldScrollForward =
                    if (lastScrolledBackward) !canScrollBackward
                    else canScrollForward

                val reachedEndWhileScrollingForward = lastScrolledForward && !canScrollForward
                val reachedStartWhileScrollingBackward = lastScrolledBackward && !canScrollBackward

                if (reachedEndWhileScrollingForward || reachedStartWhileScrollingBackward) {
                    delay(HorizontalTickerDirectionChangeDelay)
                }

                withFrameNanos {}
                scrollBy(
                    if (shouldScrollForward) HORIZONTAL_TICKER_SCROLL_DELTA
                    else -HORIZONTAL_TICKER_SCROLL_DELTA,
                )
            }
        }
    }
}

private val Trend.tickerValue
    get() = displayName ?: topic

private val LazyListState.middleItemIndex: Int
    get() {
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isEmpty()) return firstVisibleItemIndex

        val middleItem = visibleItemsInfo[visibleItemsInfo.size / 2]
        return middleItem.index
    }

private const val HORIZONTAL_TICKER_SCROLL_DELTA = 1f

private val VerticalTickerChangeDelay = 4.seconds
private val HorizontalTickerDirectionChangeDelay = 2.seconds
