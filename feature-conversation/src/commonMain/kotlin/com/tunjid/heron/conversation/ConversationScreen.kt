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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.post
import com.tunjid.heron.scaffold.navigation.profile
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.rememberPostActions
import com.tunjid.heron.timeline.ui.withQuotingPostIdPrefix
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import kotlinx.datetime.Clock
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
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(),
        modifier = modifier
            .fillMaxSize(),
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
                message = content,
                side = when {
                    content.sender.did == state.signedInProfile?.did -> Side.Sender
                    else -> Side.Receiver
                },
                isFirstMessageByAuthor = isFirstMessageByAuthor,
                isLastMessageByAuthor = isLastMessageByAuthor,
                paneScaffoldState = paneScaffoldState,
                actions = actions,
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
private fun Message(
    message: Message,
    side: Side,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
) {
    val borderColor = when (side) {
        Side.Sender -> MaterialTheme.colorScheme.primary
        Side.Receiver -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier
            .padding(
                top = if (isLastMessageByAuthor) 8.dp else 0.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .fillMaxWidth(),
        horizontalArrangement = side,
    ) {
        if (isLastMessageByAuthor) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.5.dp, borderColor, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.Top)
                    .clickable {
                        actions(
                            Action.Navigate.To(
                                profile(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                    profile = message.sender,
                                    avatarSharedElementKey = message.avatarSharedElementKey(),
                                )
                            )
                        )
                    },
            ) {
                paneScaffoldState.updatedMovableStickySharedElementOf(
                    sharedContentState = paneScaffoldState.rememberSharedContentState(
                        key = message.avatarSharedElementKey()
                    ),
                    state = remember(message.sender.avatar) {
                        ImageArgs(
                            url = message.sender.avatar?.uri,
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
        }
        // Space under avatar
        Spacer(
            modifier = Modifier
                .width(if (isLastMessageByAuthor) 16.dp else 34.dp)
        )

        AuthorAndTextMessage(
            message = message,
            side = side,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
        )

        message.post?.let { post ->
            PostMessage(
                post = post,
                message = message,
                paneScaffoldState = paneScaffoldState,
                actions = actions,
            )
        }
    }
}

@Composable
private fun AuthorAndTextMessage(
    message: Message,
    side: Side,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier
) {
    if (message.text.isNotBlank()) Column(
        modifier = modifier,
        horizontalAlignment = side,
    ) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(message)
        }
        ChatItemBubble(
            item = message,
            side = side,
        )
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
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {}) {
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
    side: Side,
) {
    val backgroundBubbleColor = when (side) {
        Side.Sender -> MaterialTheme.colorScheme.primary
        Side.Receiver -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column {
        Surface(
            color = backgroundBubbleColor,
            shape = side.bubbleShape
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


@Composable
private fun PostMessage(
    post: Post,
    message: Message,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit
) {
    Post(
        modifier = Modifier
            .padding(
                top = 16.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp
            )
            .widthIn(max = 200.dp),
        paneMovableElementSharedTransitionScope = paneScaffoldState,
        presentationLookaheadScope = paneScaffoldState,
        now = remember { Clock.System.now() },
        post = post,
        isAnchoredInTimeline = false,
        avatarShape = RoundedPolygonShape.Circle,
        sharedElementPrefix = message.conversationId.id,
        createdAt = post.createdAt,
        presentation = Timeline.Presentation.Text.WithEmbed,
        postActions = rememberPostActions(
            onPostClicked = { post, quotingPostId ->
                actions(
                    Action.Navigate.To(
                        post(
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            sharedElementPrefix = message.id.id.withQuotingPostIdPrefix(
//                                quotingPostId = quotingPostId,
                            ),
                            post = post,
                        )
                    )
                )
            },
            onProfileClicked = { profile, post, quotingPostId ->
                actions(
                    Action.Navigate.To(
                        profile(
                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            profile = profile,
                            avatarSharedElementKey = post.avatarSharedElementKey(
                                prefix = message.id.id.withQuotingPostIdPrefix(
                                    quotingPostId = quotingPostId,
                                ),
                                quotingPostId = quotingPostId,
                            ),
                        )
                    )
                )
            },
            onPostMediaClicked = { _, _, _, _ ->

            },
            onReplyToPost = {

            },
            onPostInteraction = {
                actions(Action.SendPostInteraction(it))
            },
        )
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


private sealed interface Side :
    Arrangement.Horizontal,
    Alignment.Horizontal {
    val bubbleShape: Shape

    data object Sender : Side {

        override val bubbleShape: Shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 4.dp,
            bottomEnd = 20.dp,
            bottomStart = 20.dp,
        )

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) {
            with(Arrangement.Start) {
                arrange(
                    totalSize = totalSize,
                    sizes = sizes,
                    layoutDirection = when (layoutDirection) {
                        LayoutDirection.Ltr -> LayoutDirection.Rtl
                        LayoutDirection.Rtl -> LayoutDirection.Ltr
                    },
                    outPositions = outPositions
                )
            }
        }

        override fun align(
            size: Int,
            space: Int,
            layoutDirection: LayoutDirection
        ): Int = Alignment.Start.align(
            size = size,
            space = space,
            layoutDirection = when (layoutDirection) {
                LayoutDirection.Ltr -> LayoutDirection.Rtl
                LayoutDirection.Rtl -> LayoutDirection.Ltr
            },
        )
    }

    data object Receiver :
        Side,
        Arrangement.Horizontal by Arrangement.Start,
        Alignment.Horizontal by Alignment.Start {
        override val bubbleShape: Shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 20.dp,
        )
    }
}