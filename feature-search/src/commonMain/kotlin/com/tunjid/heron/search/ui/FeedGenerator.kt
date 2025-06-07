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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.feed_by
import heron.feature_search.generated.resources.liked_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedGenerator(
    modifier: Modifier = Modifier,
    feedGenerator: FeedGenerator,
    onFeedGeneratorClicked: (FeedGenerator) -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onFeedGeneratorClicked(feedGenerator) }
            .padding(
                vertical = 4.dp,
                horizontal = 24.dp
            ),
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth(),
            avatar = {
                when(val avatar = feedGenerator.avatar) {
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
            label = {
                Text(
                    text = feedGenerator.displayName,
                    style = LocalTextStyle.current.copy(fontWeight = Bold),
                )
                Text(
                    text = stringResource(
                        Res.string.feed_by,
                        feedGenerator.creator.handle.id,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
        Spacer(
            modifier = Modifier
                .height(8.dp)
        )
        Text(
            text = remember(feedGenerator.description ?: "") {
                buildAnnotatedString {
                    val text = feedGenerator.description ?: ""

                    append(text)

                    val newlineIndices = text.indices.filter { text[it] == '\n' }
                    newlineIndices.forEach { index ->
                        addStyle(
                            style = ParagraphStyle(lineHeight = 0.1.em),
                            start = index,
                            end = index + 1,
                        )
                        addStyle(
                            style = SpanStyle(fontSize = 0.1.em),
                            start = index,
                            end = index + 1,
                        )
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp)
        )
        Text(
            text = stringResource(
                Res.string.liked_by,
                format(feedGenerator.likeCount ?: 0L)
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(
            modifier = Modifier
                .height(4.dp)
        )
    }
}

private val FeedGeneratorAvatarShape = RoundedPolygonShape.Star(
    cornerSizeAtIndex = (0..<40).map { 40.dp },
    roundingRadius = 0.32f,
)