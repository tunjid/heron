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

package com.tunjid.heron.search.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.CollectionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.feed_by
import heron.feature_search.generated.resources.liked_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedGeneratorSearchResult(
    modifier: Modifier = Modifier,
    feedGenerator: FeedGenerator,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
) {
    CollectionLayout(
        modifier = modifier
            .padding(
                vertical = 4.dp,
                horizontal = 24.dp
            ),
        title = feedGenerator.displayName,
        subtitle = stringResource(
            Res.string.feed_by,
            feedGenerator.creator.handle.id,
        ),
        description = feedGenerator.description,
        blurb = stringResource(
            Res.string.liked_by,
            format(feedGenerator.likeCount ?: 0L)
        ),
        avatar = {
            when (val avatar = feedGenerator.avatar) {
                null -> Icon(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(FeedGeneratorAvatarShape),
                    imageVector = Icons.Rounded.RssFeed,
                    contentDescription = feedGenerator.displayName,
                )

                else -> AsyncImage(
                    modifier = Modifier
                        .size(44.dp),
                    args = ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = FeedGeneratorAvatarShape,
                    )
                )
            }
        },
        onClicked = {
            onFeedGeneratorClicked(feedGenerator)
        },
    )
}

private val FeedGeneratorAvatarShape = RoundedPolygonShape.Star(
    cornerSizeAtIndex = (0..<40).map { 40.dp },
    roundingRadius = 0.32f,
)