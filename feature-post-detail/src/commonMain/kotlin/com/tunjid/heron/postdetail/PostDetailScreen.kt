/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.postdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.feed.ui.TimelineItem
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import kotlinx.datetime.Clock

@Composable
internal fun PostDetailScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.items)

    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
        )
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize(),
            state = gridState,
            columns = StaggeredGridCells.Adaptive(340.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = !movableSharedElementScope.isTransitionActive,
        ) {
            items(
                items = items,
                key = TimelineItem::id,
                itemContent = { item ->
                    TimelineItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        movableSharedElementScope = movableSharedElementScope,
                        now = remember { Clock.System.now() },
                        item = item,
                        onPostClicked = { post ->
//                            post.uri?.let {
//                                actions(
//                                    Action.Navigate.ToPost(
//                                        profileId = post.author.did,
//                                        postId = post.cid,
//                                        postUri = it,
//                                    )
//                                )
//                            }
                        },
                        onProfileClicked = { profile ->
//                            actions(
//                                Action.Navigate.ToProfile(
//                                    profileId = profile.did,
//                                    profileAvatar = profile.avatar,
//                                    avatarSharedElementKey = this?.avatarSharedElementKey(
//                                        item.sourceId,
//                                    )
//                                )
//                            )
                        },
                        onImageClicked = {},
                        onReplyToPost = {},
                    )
                }
            )
        }
    }
}

