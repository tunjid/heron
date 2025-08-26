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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.dragToPop
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

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

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        paneScaffoldState.updatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = state.avatarSharedElementKey ?: "",
                )
            },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f)
                .dragToPop(),
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
