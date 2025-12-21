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

package com.tunjid.heron.timeline.ui.feed

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Timeline.Update
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.FeedGeneratorCollectionShape
import com.tunjid.heron.timeline.utilities.TimelineStatusSelection
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.timeline.utilities.orDefault
import com.tunjid.heron.ui.RecordLayout
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.feed_by
import heron.ui.timeline.generated.resources.liked_by
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedGenerator(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    feedGenerator: FeedGenerator,
    status: Timeline.Home.Status?,
    onFeedGeneratorStatusUpdated: (Update.OfFeedGenerator) -> Unit,
) = with(movableElementSharedTransitionScope) {
    RecordLayout(
        modifier = modifier,
        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
        title = feedGenerator.displayName,
        subtitle = stringResource(
            Res.string.feed_by,
            feedGenerator.creator.handle.id,
        ),
        description = feedGenerator.description,
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = feedGenerator.uri,
        blurb = stringResource(
            Res.string.liked_by,
            format(feedGenerator.likeCount ?: 0L),
        ),
        avatar = {
            val avatar = feedGenerator.avatar.orDefault
            PaneStickySharedElement(
                modifier = Modifier
                    .size(44.dp),
                sharedContentState = rememberSharedContentState(
                    key = feedGenerator.avatarSharedElementKey(sharedElementPrefix),
                ),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap(),
                    args = remember(avatar) {
                        ImageArgs(
                            url = avatar.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            shape = FeedGeneratorCollectionShape,
                        )
                    },
                )
            }
        },
        action = {
            status?.let { currentStatus ->
                FeedGeneratorStatus(
                    status = currentStatus,
                    uri = feedGenerator.uri,
                    onFeedGeneratorStatusUpdated = onFeedGeneratorStatusUpdated,
                )
            }
        },
    )
}

@Composable
fun FeedGeneratorStatus(
    status: Timeline.Home.Status,
    uri: FeedGeneratorUri,
    onFeedGeneratorStatusUpdated: (Update.OfFeedGenerator) -> Unit,
) {
    TimelineStatusSelection(
        currentStatus = status,
        onStatusSelected = { selectedStatus ->
            val update = when (selectedStatus) {
                Timeline.Home.Status.Pinned -> Update.OfFeedGenerator.Pin(uri)
                Timeline.Home.Status.Saved -> Update.OfFeedGenerator.Save(uri)
                Timeline.Home.Status.None -> Update.OfFeedGenerator.Remove(uri)
            }
            onFeedGeneratorStatusUpdated(update)
        },
    )
}
