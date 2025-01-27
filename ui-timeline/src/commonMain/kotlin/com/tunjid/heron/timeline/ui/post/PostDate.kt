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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.utilities.formatDate
import com.tunjid.heron.timeline.utilities.formatTime
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.like
import heron.ui_timeline.generated.resources.likes
import heron.ui_timeline.generated.resources.quote
import heron.ui_timeline.generated.resources.quotes
import heron.ui_timeline.generated.resources.repost
import heron.ui_timeline.generated.resources.reposts
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostMetadata(
    modifier: Modifier = Modifier,
    time: Instant,
    postId: Id,
    profileId: Id,
    reposts: Long,
    quotes: Long,
    likes: Long,
    onMetadataClicked: (Post.Metadata) -> Unit,
) {
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.outline
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = modifier,
            text = "${time.formatDate()} • ${time.formatTime()}",
            style = textStyle,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetadataText(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = {
                            onMetadataClicked(
                                Post.Metadata.Reposts(
                                    profileId = profileId,
                                    postId = postId
                                )
                            )
                        },
                    ),
                count = reposts,
                singularResource = Res.string.repost,
                pluralResource = Res.string.reposts,
                textStyle = textStyle,
            )
            MetadataText(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = {
                            onMetadataClicked(
                                Post.Metadata.Quotes(
                                    profileId = profileId,
                                    postId = postId,
                                )
                            )
                        },
                    ),
                count = quotes,
                singularResource = Res.string.quote,
                pluralResource = Res.string.quotes,
                textStyle = textStyle,
            )
            MetadataText(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = {
                            onMetadataClicked(
                                Post.Metadata.Likes(
                                    profileId = profileId,
                                    postId = postId,
                                )
                            )
                        },
                    ),
                count = likes,
                singularResource = Res.string.like,
                pluralResource = Res.string.likes,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
internal fun MetadataText(
    modifier: Modifier = Modifier,
    count: Long,
    singularResource: StringResource,
    pluralResource: StringResource,
    textStyle: TextStyle,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = count.toString(),
            style = textStyle.copy(
                fontWeight = FontWeight.Bold,
            )
        )
        Text(
            text = stringResource(
                if (count == 1L) singularResource
                else pluralResource
            ),
            style = textStyle,
        )
    }
}