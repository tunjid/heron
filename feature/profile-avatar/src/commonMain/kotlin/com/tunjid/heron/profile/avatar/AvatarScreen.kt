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

package com.tunjid.heron.profile.avatar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.composables.gesturezoom.GestureZoomState.Companion.gestureZoomable
import com.tunjid.composables.gesturezoom.GestureZoomState.Options
import com.tunjid.composables.gesturezoom.rememberGestureZoomState
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.dragToPop
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.rememberDragToPopState
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AvatarScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    @Suppress("UNUSED_PARAMETER")
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    val avatar = profile?.avatar

    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberGestureZoomState(
        zoomScale = DefaultZoomScale,
        options = remember {
            Options(
                scale = Options.Scale.Layout,
                offset = Options.Offset.Layout,
            )
        },
    )
    Box(
        modifier = modifier
            .dragToPop(
                rememberDragToPopState {
                    !zoomState.enabled || zoomState.zoomScale == DefaultZoomScale
                },
            )
            .fillMaxSize(),
    ) {
        paneScaffoldState.UpdatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = state.avatarSharedElementKey ?: "",
                )
            },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f)
                .gestureZoomable(zoomState)
                .combinedClickable(
                    onClick = {},
                    onDoubleClick = {
                        coroutineScope.launch { zoomState.toggleZoom() }
                    },
                ),
            state = remember(avatar) {
                ImageArgs(
                    url = avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile?.displayName ?: profile?.handle?.id,
                    shape = RoundedPolygonShape.Circle,
                )
            },
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
            },
        )
    }
}

private const val DefaultZoomScale = 1f
