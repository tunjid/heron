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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.timeline.ui.post.PostMetadataText.Companion.pluralStringResource
import com.tunjid.heron.timeline.ui.post.PostMetadataText.Companion.singularStringResource
import com.tunjid.heron.timeline.utilities.formatDate
import com.tunjid.heron.timeline.utilities.formatTime
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.like
import heron.ui.timeline.generated.resources.likes
import heron.ui.timeline.generated.resources.post_replies_all
import heron.ui.timeline.generated.resources.post_replies_none
import heron.ui.timeline.generated.resources.post_replies_some
import heron.ui.timeline.generated.resources.quote
import heron.ui.timeline.generated.resources.quotes
import heron.ui.timeline.generated.resources.repost
import heron.ui.timeline.generated.resources.reposts
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostMetadata(
    modifier: Modifier = Modifier,
    time: Instant,
    replyStatus: PostReplyStatus,
    postUri: PostUri,
    profileId: ProfileId,
    reposts: Long,
    quotes: Long,
    likes: Long,
    onMetadataClicked: (PostMetadata) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.outline
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = textColor,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier,
                text = "${time.formatDate()} • ${time.formatTime()}",
                style = textStyle,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onMetadataClicked(PostMetadata.Gate(postUri))
                    },
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = replyStatus.icon,
                    contentDescription = null,
                    tint = textColor,
                )
                Text(
                    modifier = Modifier,
                    text = stringResource(replyStatus.stringRes),
                    style = textStyle,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PostMetadataText.All.forEach { metadataText ->
                MetadataText(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = {
                                onMetadataClicked(
                                    when (metadataText) {
                                        PostMetadataText.Likes -> PostMetadata.Likes(
                                            profileId = profileId,
                                            postRecordKey = postUri.recordKey,
                                        )

                                        PostMetadataText.Quotes -> PostMetadata.Quotes(
                                            profileId = profileId,
                                            postRecordKey = postUri.recordKey,
                                        )

                                        PostMetadataText.Reposts -> PostMetadata.Reposts(
                                            profileId = profileId,
                                            postRecordKey = postUri.recordKey,
                                        )
                                    },
                                )
                            },
                        ),
                    count = when (metadataText) {
                        PostMetadataText.Likes -> likes
                        PostMetadataText.Quotes -> quotes
                        PostMetadataText.Reposts -> reposts
                    },
                    singularResource = metadataText.singularStringResource,
                    pluralResource = metadataText.pluralStringResource,
                    textStyle = textStyle,
                )
            }
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
            ),
        )
        Text(
            text = stringResource(
                if (count == 1L) singularResource
                else pluralResource,
            ),
            style = textStyle,
        )
    }
}

internal enum class PostReplyStatus {
    All,
    Some,
    None,
}

private val PostReplyStatus.stringRes
    get() = when (this) {
        PostReplyStatus.All -> Res.string.post_replies_all
        PostReplyStatus.Some -> Res.string.post_replies_some
        PostReplyStatus.None -> Res.string.post_replies_none
    }

private val PostReplyStatus.icon
    get() = when (this) {
        PostReplyStatus.All -> Icons.Rounded.Public
        PostReplyStatus.Some -> Icons.Rounded.Groups
        PostReplyStatus.None -> Icons.Rounded.Block
    }

sealed class PostMetadata {

    data class Likes(
        val profileId: ProfileId,
        val postRecordKey: RecordKey,
    ) : PostMetadata()

    data class Reposts(
        val profileId: ProfileId,
        val postRecordKey: RecordKey,
    ) : PostMetadata()

    data class Quotes(
        val profileId: ProfileId,
        val postRecordKey: RecordKey,
    ) : PostMetadata()

    data class Gate(
        val postUri: PostUri,
    ) : PostMetadata()
}

private sealed class PostMetadataText {

    data object Reposts : PostMetadataText()
    data object Quotes : PostMetadataText()
    data object Likes : PostMetadataText()

    companion object {

        val PostMetadataText.singularStringResource
            get() = when (this) {
                Reposts -> Res.string.repost
                Quotes -> Res.string.quote
                Likes -> Res.string.like
            }

        val PostMetadataText.pluralStringResource
            get() = when (this) {
                Reposts -> Res.string.reposts
                Quotes -> Res.string.quotes
                Likes -> Res.string.likes
            }

        val All = listOf(
            Reposts,
            Quotes,
            Likes,
        )
    }
}
