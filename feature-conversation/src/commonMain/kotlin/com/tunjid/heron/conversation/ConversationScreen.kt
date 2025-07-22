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

package com.tunjid.heron.conversation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ConversationScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.tiledItems)

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier
            .fillMaxSize()
    ) {
        items(
            count = items.size,
            key = { items[it].id.id }
        ) { index ->
            val prevAuthor = items.getOrNull(index - 1)?.sender
            val nextAuthor = items.getOrNull(index + 1)?.sender
            val content = items[index]
            val isFirstMessageByAuthor = prevAuthor != content.sender
            val isLastMessageByAuthor = nextAuthor != content.sender

            Message(
                onAuthorClick = { message ->
                    actions(
                        Action.Navigate.DelegateTo(
                            NavigationAction.Common.ToProfile(
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                profile = message.sender,
                                avatarSharedElementKey = message.avatarSharedElementKey(),
                            )
                        )
                    )
                },
                item = content,
                isUserMe = content.sender.did == state.signedInProfile?.did,
                isFirstMessageByAuthor = isFirstMessageByAuthor,
                isLastMessageByAuthor = isLastMessageByAuthor,
                paneScaffoldState = paneScaffoldState
            )
        }
    }


    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query = query ?: state.tilingData.currentQuery
                    )
                )
            )
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Message(
    onAuthorClick: (Message) -> Unit,
    item: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    paneScaffoldState: PaneScaffoldState,
) {
    val borderColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier

    Row(modifier = spaceBetweenAuthors) {
        if (isLastMessageByAuthor) {
            // Avatar
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(42.dp)
                    .border(1.5.dp, borderColor, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.Top)
                    .clickable {
                        onAuthorClick(item)
                    },
            ) {
                paneScaffoldState.updatedMovableStickySharedElementOf(
                    sharedContentState = paneScaffoldState.rememberSharedContentState(
                        key = item.avatarSharedElementKey()
                    ),
                    state = remember(item.sender.avatar) {
                        ImageArgs(
                            url = item.sender.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                    modifier = Modifier.matchParentSize(),
                    sharedElement = { args, innerModifier ->
                        AsyncImage(args, innerModifier)
                    }
                )
            }
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.width(74.dp))
        }
        AuthorAndTextMessage(
            item = item,
            isUserMe = isUserMe,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    item: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(item)
        }
        ChatItemBubble(item, isUserMe)
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(
    item: Message
) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = item.sender.displayName ?: "",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.sentAt.toTimestamp(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatItemBubble(
    item: Message,
    isUserMe: Boolean
) {
    val backgroundBubbleColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column {
        Surface(
            color = backgroundBubbleColor,
            shape = ChatBubbleShape
        ) {
            ChatMessage(
                message = item,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ChatMessage(
    message: Message,
) {
    Text(
        text = message.text,
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
        modifier = Modifier.padding(16.dp),
    )
}

private fun Message.avatarSharedElementKey(): String {
    return "${id.id}-${conversationId.id}-${sender.did.id}"
}

private fun Instant.toTimestamp(): String {
    // Convert Instant to LocalDateTime in the system's default time zone
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

    val minute = if (localDateTime.minute < 10) "0${localDateTime.minute}" else localDateTime.minute
    val amOrPm = if (localDateTime.hour > 12) "PM" else "AM"
    return "${localDateTime.hour}.$minute $amOrPm"
}

private val ChatBubbleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp,
    bottomStart = 20.dp,
)