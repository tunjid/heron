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

package com.tunjid.heron.timeline.utilities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.PostModerationTools
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.asClipEntry
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.copy_link_to_clipboard
import heron.ui.timeline.generated.resources.send_via_direct_message
import heron.ui.timeline.generated.resources.share_in_post
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SendDirectMessageCard(
    signedInProfileId: ProfileId,
    recentConversations: List<Conversation>,
    onConversationClicked: (Conversation) -> Unit,
) {
    BottomSheetItemCard(
        content = {
            Text(
                modifier = Modifier
                    .padding(
                        vertical = 4.dp,
                    ),
                text = stringResource(Res.string.send_via_direct_message),
                style = MaterialTheme.typography.bodySmall,
            )
            LazyRow(
                modifier = Modifier
                    .clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = recentConversations,
                    key = { it.id.id },
                ) { conversation ->
                    val member = conversation.members.firstOrNull {
                        it.did != signedInProfileId
                    } ?: return@items
                    AsyncImage(
                        args = remember(member.avatar?.uri) {
                            ImageArgs(
                                url = member.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                onConversationClicked(conversation)
                            },
                    )
                }
            }
        },
    )
}

@Composable
internal fun ShareInPostCard(
    onShareInPostClicked: () -> Unit,
) {
    val shareInPostDescription = stringResource(Res.string.share_in_post)

    BottomSheetItemCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onShareInPostClicked,
    ) {
        BottomSheetItemCardRow(
            modifier = Modifier
                .semantics {
                    contentDescription = shareInPostDescription
                },
            icon = Icons.AutoMirrored.Rounded.Article,
            text = shareInPostDescription,
        )
    }
}

@Composable
internal fun CopyToClipboardCard(
    uri: GenericUri,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyToClipboardDescription = stringResource(Res.string.copy_link_to_clipboard)

    BottomSheetItemCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                clipboard.setClipEntry(uri.asClipEntry(copyToClipboardDescription))
            }
        },
    ) {
        BottomSheetItemCardRow(
            modifier = Modifier
                .semantics {
                    contentDescription = copyToClipboardDescription
                },
            icon = Icons.Rounded.ContentCopy,
            text = copyToClipboardDescription,
        )
    }
}

@Composable
internal fun PostModerationMenuSection(
    modifier: Modifier = Modifier,
    signedInProfileId: ProfileId,
    post: Post,
    onOptionClicked: (PostOption) -> Unit,
) {
    ModerationMenuCard(modifier) {
        PostModerationTools.entries.forEachIndexed { index, tool ->
            val isLast = index == PostModerationTools.entries.lastIndex

            BottomSheetItemCardRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val option = when (tool) {
                            PostModerationTools.MuteWords -> PostOption.Moderation.MuteWords
                            PostModerationTools.BlockAccount -> PostOption.Moderation.BlockAccount(
                                signedInProfileId = signedInProfileId,
                                post = post,
                            )
                            PostModerationTools.MuteAccount -> PostOption.Moderation.MuteAccount(
                                signedInProfileId = signedInProfileId,
                                post = post,
                            )
                        }
                        onOptionClicked(option)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                icon = tool.icon,
                text = stringResource(tool.stringRes),
                isModerationItem = true,
            )
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 0.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
internal fun ModerationMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BottomSheetItemCard(
        modifier = modifier,
        isModerationMenu = true,
        onClick = null, // Card itself is not clickable, only items inside
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
internal fun BottomSheetItemCard(
    modifier: Modifier = Modifier,
    isModerationMenu: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    if (onClick == null) ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        BottomSheetItemCardColumn(
            content = content,
            isModerationMenu = isModerationMenu,
        )
    }
    else ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
    ) {
        BottomSheetItemCardColumn(
            content = content,
            isModerationMenu = isModerationMenu,
        )
    }
}

@Composable
internal inline fun BottomSheetItemCardRow(
    modifier: Modifier = Modifier,
    isModerationItem: Boolean = false,
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isModerationItem) 0.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private inline fun BottomSheetItemCardColumn(
    isModerationMenu: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isModerationMenu) 0.dp else 16.dp,
                vertical = 8.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}
