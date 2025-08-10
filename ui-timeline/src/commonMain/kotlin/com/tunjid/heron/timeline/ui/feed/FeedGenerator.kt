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
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.FeedGeneratorCollectionShape
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.CollectionLayout
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.feed_by
import heron.ui_timeline.generated.resources.liked_by
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedGenerator(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    feedGenerator: FeedGenerator,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
) = with(movableElementSharedTransitionScope) {
    CollectionLayout(
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
            format(feedGenerator.likeCount ?: 0L)
        ),
        avatar = {
            val avatar = feedGenerator.avatar ?: BlueskyClouds
            AsyncImage(
                modifier = Modifier
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = feedGenerator.avatarSharedElementKey(sharedElementPrefix)
                        )
                    )
                    .size(44.dp),
                args = remember(avatar) {
                    ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = FeedGeneratorCollectionShape,
                    )
                }
            )
        },
        onClicked = {
            onFeedGeneratorClicked(feedGenerator)
        },
    )
}