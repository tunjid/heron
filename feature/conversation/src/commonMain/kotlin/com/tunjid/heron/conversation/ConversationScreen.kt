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
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.conversation.ui.EmojiPickerBottomSheet
import com.tunjid.heron.conversation.ui.EmojiPickerSheetState.Companion.rememberEmojiPickerState
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.EmbeddedRecord
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.rememberFormattedTextPost
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.UpdatedMovableStickySharedElementOf
import kotlin.time.Instant
import kotlinx.coroutines.flow.drop
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

    val emojiPickerSheetState = rememberEmojiPickerState()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(),
        modifier = modifier
            .fillMaxSize(),
    ) {
        items(
            count = items.size,
            key = { items[it].id },
        ) { index ->
            val prevAuthor = items.getOrNull(index - 1)?.sender
            val nextAuthor = items.getOrNull(index + 1)?.sender
            val content = items[index]
            val isFirstMessageByAuthor = prevAuthor != content.sender
            val isLastMessageByAuthor = nextAuthor != content.sender

            Message(
                modifier = Modifier
                    .animateItem(),
                item = content,
                side = when {
                    content.sender.did == state.signedInProfile?.did -> Side.Sender
                    else -> Side.Receiver
                },
                isFirstMessageByAuthor = isFirstMessageByAuthor,
                isLastMessageByAuthor = isLastMessageByAuthor,
                paneScaffoldState = paneScaffoldState,
                actions = actions,
                onMessageLongPressed = { item ->
                    when (item) {
                        is MessageItem.Pending -> Unit
                        is MessageItem.Sent -> emojiPickerSheetState.showSheet(item.message)
                    }
                },
                onLinkTargetClicked = { linkTarget ->
                    if (linkTarget is LinkTarget.Navigable) actions(
                        Action.Navigate.To(
                            pathDestination(
                                path = linkTarget.path,
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )
                },
            )
        }
    }

    EmojiPickerBottomSheet(
        state = emojiPickerSheetState,
        onEmojiSelected = { message, emoji ->
            actions(
                Action.UpdateMessageReaction(
                    when {
                        message.hasEmojiReaction(emoji) -> Message.UpdateReaction.Remove(
                            value = emoji,
                            messageId = message.id,
                            convoId = message.conversationId,
                        )

                        else -> Message.UpdateReaction.Add(
                            value = emoji,
                            messageId = message.id,
                            convoId = message.conversationId,
                        )
                    },
                ),
            )
        },
    )

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query = query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )

    LaunchedEffect(listState) {
        snapshotFlow { items.firstOrNull()?.id }
            .drop(1)
            .collect {
                // User has scrolled to see earlier chats
                if (listState.lastScrolledForward) return@collect
                // Not close enough to the bottom
                if (listState.firstVisibleItemIndex !in 0..4) return@collect
                // Scroll to bottom
                listState.animateScrollToItem(0)
            }
    }
}

@Composable
private fun Message(
    modifier: Modifier = Modifier,
    item: MessageItem,
    side: Side,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
    onMessageLongPressed: (MessageItem) -> Unit,
    onLinkTargetClicked: (LinkTarget) -> Unit,
) {
    val borderColor = when (side) {
        Side.Sender -> MaterialTheme.colorScheme.primary
        Side.Receiver -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = modifier
            .padding(
                top = if (isLastMessageByAuthor) 8.dp else 0.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .fillMaxWidth(),
        horizontalArrangement = side,
    ) {
        if (isLastMessageByAuthor) {
            MessageAvatar(
                modifier = Modifier.size(24.dp)
                    .border(1.5.dp, borderColor, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.Top)
                    .clickable {
                        actions(
                            Action.Navigate.To(
                                profileDestination(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                    profile = item.sender,
                                    avatarSharedElementKey = item.avatarSharedElementKey(),
                                ),
                            ),
                        )
                    },
                item = item,
                paneScaffoldState = paneScaffoldState,
            )
        }
        Spacer(
            modifier = Modifier
                .width(if (isLastMessageByAuthor) 16.dp else 34.dp),
        )

        AuthorAndTextMessage(
            item = item,
            side = side,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier,
            onMessageLongPressed = onMessageLongPressed,
            paneScaffoldState = paneScaffoldState,
            onLinkTargetClicked = onLinkTargetClicked,
        )

        when (item) {
            is MessageItem.Pending -> Unit
            is MessageItem.Sent -> item.message.embeddedRecord?.let { record ->
                MessageRecord(
                    record = record,
                    item = item,
                    paneScaffoldState = paneScaffoldState,
                    actions = actions,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MessageAvatar(
    modifier: Modifier = Modifier,
    item: MessageItem,
    paneScaffoldState: PaneScaffoldState,
) {
    Box(
        modifier = modifier,
    ) {
        paneScaffoldState.UpdatedMovableStickySharedElementOf(
            sharedContentState = paneScaffoldState.rememberSharedContentState(
                key = item.avatarSharedElementKey(),
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
            },
        )
    }
}

@Composable
private fun AuthorAndTextMessage(
    modifier: Modifier = Modifier,
    item: MessageItem,
    side: Side,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    paneScaffoldState: PaneScaffoldState,
    onMessageLongPressed: (MessageItem) -> Unit,
    onLinkTargetClicked: (LinkTarget) -> Unit,
) {
    if (item.text.isNotBlank()) Column(
        modifier = modifier,
        horizontalAlignment = side,
    ) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(item)
        }
        ChatItemBubble(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onMessageLongPressed(item)
                        },
                    )
                },
            message = item,
            side = side,
            paneScaffoldState = paneScaffoldState,
            onLinkTargetClicked = onLinkTargetClicked,
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
    item: MessageItem,
) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = item.sender.displayName ?: "",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp), // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.sentAt.toTimestamp(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ChatItemBubble(
    modifier: Modifier = Modifier,
    message: MessageItem,
    side: Side,
    paneScaffoldState: PaneScaffoldState,
    onLinkTargetClicked: (LinkTarget) -> Unit,
) {
    val backgroundBubbleColor = when (side) {
        Side.Sender -> MaterialTheme.colorScheme.primary
        Side.Receiver -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = modifier,
        horizontalAlignment = side,
    ) {
        Surface(
            color = backgroundBubbleColor,
            shape = side.bubbleShape,
        ) {
            Text(
                text = rememberFormattedTextPost(
                    text = message.text,
                    textLinks = message.links,
                    textLinkStyles = side.rememberTextLinkStyle(),
                    onLinkTargetClicked = onLinkTargetClicked,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                modifier = Modifier.padding(
                    vertical = 8.dp,
                    horizontal = 16.dp,
                ),
            )
        }

        if (message.reactions.isNotEmpty()) Row(
            modifier = Modifier
                .offset(
                    x = when (side) {
                        Side.Receiver -> 16.dp
                        Side.Sender -> (-16).dp
                    },
                    y = (-8).dp,
                )
                .background(
                    color = MaterialTheme.colorScheme.outline,
                    shape = ReactionsChipShape,
                )
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            message.reactions.forEach { reaction ->
                key(reaction.value) {
                    Text(
                        modifier = Modifier
                            .animateBounds(paneScaffoldState),
                        text = reaction.value,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun MessageRecord(
    record: Record,
    item: MessageItem,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
) {
    EmbeddedRecord(
        modifier = Modifier
            .padding(
                top = 16.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .widthIn(max = 200.dp),
        record = record,
        sharedElementPrefix = item.id,
        movableElementSharedTransitionScope = paneScaffoldState,
        postActions = remember(item.id, actions) {
            postActions(
                onLinkTargetClicked = { _, linkTarget ->
                    if (linkTarget is LinkTarget.Navigable) actions(
                        Action.Navigate.To(
                            pathDestination(
                                path = linkTarget.path,
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )
                },
                onPostClicked = { post ->
                    actions(
                        Action.Navigate.To(
                            recordDestination(
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                sharedElementPrefix = item.id,
                                record = post,
                            ),
                        ),
                    )
                },
                onProfileClicked = { profile, post, quotingPostUri ->
                    actions(
                        Action.Navigate.To(
                            profileDestination(
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                profile = profile,
                                avatarSharedElementKey = post.avatarSharedElementKey(
                                    prefix = item.id.withQuotingPostUriPrefix(
                                        quotingPostUri = quotingPostUri,
                                    ),
                                    quotingPostUri = quotingPostUri,
                                ),
                            ),
                        ),
                    )
                },
                onPostRecordClicked = { record, owningPostUri ->
                    actions(
                        Action.Navigate.To(
                            recordDestination(
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                sharedElementPrefix = item.id.withQuotingPostUriPrefix(
                                    quotingPostUri = owningPostUri,
                                ),
                                record = record,
                            ),
                        ),
                    )
                },
                onPostMediaClicked = { media: Embed.Media, index: Int, post: Post, quotingPostUri: PostUri? ->
                    actions(
                        Action.Navigate.To(
                            galleryDestination(
                                post = post,
                                media = media,
                                startIndex = index,
                                sharedElementPrefix = item.id.withQuotingPostUriPrefix(
                                    quotingPostUri = quotingPostUri,
                                ),
                            ),
                        ),
                    )
                },
                onReplyToPost = {
                },
                onPostInteraction = { interaction, _ ->
                    actions(Action.SendPostInteraction(interaction))
                },
                onPostOptionsClicked = {
                },
            )
        },
    )
}

private fun MessageItem.avatarSharedElementKey(): String {
    return "$id-${conversationId.id}-${sender.did.id}"
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
            outPositions: IntArray,
        ) {
            with(Arrangement.Start) {
                arrange(
                    totalSize = totalSize,
                    sizes = sizes,
                    layoutDirection = when (layoutDirection) {
                        LayoutDirection.Ltr -> LayoutDirection.Rtl
                        LayoutDirection.Rtl -> LayoutDirection.Ltr
                    },
                    outPositions = outPositions,
                )
            }
        }

        override fun align(
            size: Int,
            space: Int,
            layoutDirection: LayoutDirection,
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

@Composable
private fun Side.rememberTextLinkStyle() = when (this) {
    Side.Receiver -> null
    Side.Sender -> {
        val color = MaterialTheme.colorScheme.onPrimary
        remember(color) {
            TextLinkStyles(
                style = SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline,
                ),
            )
        }
    }
}

private val ReactionsChipShape = RoundedCornerShape(16.dp)
