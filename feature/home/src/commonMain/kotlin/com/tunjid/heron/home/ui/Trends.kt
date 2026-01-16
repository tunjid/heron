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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Trend
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun Trends(
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    trends: List<Trend>,
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
                var isExpanded by rememberSaveable {
                    mutableStateOf(false)
                }

                AnimatedContent(
                    targetState = isExpanded,
                ) { currentlyExpanded ->
                    if (currentlyExpanded) TrendsChyron(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@AnimatedContent,
                        trends = trends,
                        onClick = { isExpanded = false },
                    ) else TrendsButton(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@AnimatedContent,
                        trends = trends,
                        onClick = { isExpanded = true },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendsButton(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    trends: List<Trend>,
    onClick: () -> Unit,
) = with(sharedTransitionScope) {
    Row(
        modifier = modifier
            .padding(
                horizontal = 12.dp,
            )
            .fillMaxHeight()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var index by remember { mutableStateOf(0) }
        val trend = trends[index % trends.size]
        val text = trend.displayName ?: ""

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
            targetState = text,
            transitionSpec = { TextCheckedTransform },
        ) { currentText ->
            Text(
                modifier = Modifier,
                text = currentText,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMediumEmphasized,
            )
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(4.seconds)
                ++index
            }
        }
    }
}

@Composable
private fun TrendsChyron(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    trends: List<Trend>,
    onClick: () -> Unit,
) = with(sharedTransitionScope) {
    val state = rememberLazyListState()

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        state = state,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                            .padding(4.dp),
                        text = it,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall,
                    )

                }
            },
        )
    }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) return@LaunchedEffect

        while (isActive) {
            withFrameNanos {}
            state.scrollBy(.5f * if (state.canScrollForward) 1f else -1f)
        }
    }
}

private const val ICON_ANIMATION_DURATION_MILLIS = 600

private val TextAnimationSpec = tween<IntOffset>(ICON_ANIMATION_DURATION_MILLIS)

private val TextCheckedTransform = slideInVertically(
    animationSpec = TextAnimationSpec,
    initialOffsetY = { it },
) togetherWith slideOutVertically(
    animationSpec = TextAnimationSpec,
    targetOffsetY = { -it },
)

