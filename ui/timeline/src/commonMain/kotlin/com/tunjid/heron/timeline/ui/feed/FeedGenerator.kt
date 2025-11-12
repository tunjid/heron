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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Timeline.Update
import com.tunjid.heron.data.core.models.Timeline.Update.OfFeedGenerator.Pin
import com.tunjid.heron.data.core.models.Timeline.Update.OfFeedGenerator.Remove
import com.tunjid.heron.data.core.models.Timeline.Update.OfFeedGenerator.Save
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.FeedGeneratorCollectionShape
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.ItemSelection
import com.tunjid.heron.ui.RecordLayout
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.feed_by
import heron.ui.timeline.generated.resources.liked_by
import heron.ui.timeline.generated.resources.more_options
import heron.ui.timeline.generated.resources.pin_feed
import heron.ui.timeline.generated.resources.remove_feed
import heron.ui.timeline.generated.resources.save_feed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedGenerator(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    feedGenerator: FeedGenerator,
    status: FeedGenerator.Status?,
    onFeedGeneratorStatusUpdated: (Update) -> Unit,
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
            val avatar = feedGenerator.avatar ?: BlueskyClouds
            AsyncImage(
                modifier = Modifier
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = feedGenerator.avatarSharedElementKey(sharedElementPrefix),
                        ),
                    )
                    .size(44.dp),
                args = remember(avatar) {
                    ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = FeedGeneratorCollectionShape,
                    )
                },
            )
        },
        action = {
            status?.let {
                FeedGeneratorStatus(
                    status = it,
                    feedGenerator = feedGenerator,
                    onFeedGeneratorStatusUpdated = onFeedGeneratorStatusUpdated,
                )
            }
        },
    )
}

@Composable
fun FeedGeneratorStatus(
    status: FeedGenerator.Status,
    feedGenerator: FeedGenerator,
    onFeedGeneratorStatusUpdated: (Update) -> Unit,
) {
    ItemSelection(
        selectedItem = status,
        availableItems = remember { FeedGenerator.Status.entries },
        key = FeedGenerator.Status::name,
        icon = FeedGenerator.Status::icon,
        stringResource = FeedGenerator.Status::textResource,
        onItemSelected = { selectedStatus ->
            val update = when (selectedStatus) {
                FeedGenerator.Status.Pinned -> Update.OfFeedGenerator.Pin(feedGenerator.uri)
                FeedGenerator.Status.Saved -> Update.OfFeedGenerator.Save(feedGenerator.uri)
                FeedGenerator.Status.None -> Update.OfFeedGenerator.Remove(feedGenerator.uri)
            }
            onFeedGeneratorStatusUpdated(update)
        },
    )
}

private fun FeedGenerator.Status.textResource(): StringResource =
    when (this) {
        FeedGenerator.Status.Pinned -> Res.string.pin_feed
        FeedGenerator.Status.Saved -> Res.string.save_feed
        FeedGenerator.Status.None -> Res.string.remove_feed
    }

private val FeedGenerator.Status.icon: ImageVector
    get() = when (this) {
        FeedGenerator.Status.Pinned -> Icons.Rounded.Star
        FeedGenerator.Status.Saved -> Icons.Rounded.Bookmark
        FeedGenerator.Status.None -> Icons.Outlined.BookmarkAdd
    }
