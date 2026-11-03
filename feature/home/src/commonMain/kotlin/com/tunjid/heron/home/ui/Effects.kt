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

import androidx.compose.animation.core.animate
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.ui.UiTokens
import kotlinx.coroutines.flow.first

@Composable
fun PagerState.RestoreLastViewedTabEffect(
    lastViewedTabUri: Uri?,
    timelines: List<Timeline.Home>,
) {
    val updatedTimelines = rememberUpdatedState(lastViewedTabUri to timelines)
    LaunchedEffect(Unit) {
        val (lastTabUri, initialTimelines) = snapshotFlow { updatedTimelines.value }
            .first { (_, timelines) -> timelines.isNotEmpty() }

        val page = initialTimelines.indexOfFirst { it.uri == lastTabUri }
        if (page < 0) return@LaunchedEffect
        if (!initialTimelines[page].isPinned) return@LaunchedEffect

        scrollToPage(page)
    }
}

@Composable
internal fun TabsExpansionEffect(
    isExpanded: Boolean,
    onDisplaced: (Float) -> Unit,
) {
    val updatedOnDisplaced by rememberUpdatedState(onDisplaced)
    val density = LocalDensity.current
    val expandedHeight = rememberUpdatedState(
        with(density) {
            (UiTokens.statusBarHeight + UiTokens.toolbarHeight).toPx()
        },
    )

    LaunchedEffect(isExpanded) {
        if (!isExpanded) return@LaunchedEffect

        var cumulative = 0f
        animate(
            initialValue = cumulative,
            targetValue = expandedHeight.value,
        ) { current, _ ->
            val delta = current - cumulative
            cumulative += delta
            updatedOnDisplaced(delta)
        }
    }
}
