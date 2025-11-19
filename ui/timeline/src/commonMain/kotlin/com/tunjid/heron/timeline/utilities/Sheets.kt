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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
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
    ShareActionCard(
        showDivider = false,
        topContent = {
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
        bottomContent = {
        },
    )
}

@Composable
internal fun ShareInPostCard(
    onShareInPostClicked: () -> Unit,
) {
    val shareInPostDescription = stringResource(Res.string.share_in_post)

    ShareActionCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onShareInPostClicked,
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = shareInPostDescription
                }
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shareInPostDescription,
                style = MaterialTheme.typography.bodyLarge,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CopyToClipboardCard(
    uri: GenericUri,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyToClipboardDescription = stringResource(Res.string.copy_link_to_clipboard)

    ShareActionCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                clipboard.setClipEntry(uri.asClipEntry(copyToClipboardDescription))
            }
        },
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = copyToClipboardDescription
                }
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = copyToClipboardDescription,
                style = MaterialTheme.typography.bodyLarge,
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShareActionCard(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    ShareActionCard(
        modifier = modifier,
        onClick = null,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                topContent?.invoke(this)

                if (showDivider) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.6.dp,
                    )
                }
                bottomContent()
            }
        },
    )
}

@Composable
private fun ShareActionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    if (onClick == null) ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
    else ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}
