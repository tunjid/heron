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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.composables.ui.animate
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Post.Interaction.Create.*
import com.tunjid.heron.data.core.models.Post.Interaction.Delete.*
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.icon
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.stringResource
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.bookmarked
import heron.ui.timeline.generated.resources.cancel
import heron.ui.timeline.generated.resources.download
import heron.ui.timeline.generated.resources.liked
import heron.ui.timeline.generated.resources.quote
import heron.ui.timeline.generated.resources.reply
import heron.ui.timeline.generated.resources.repost
import heron.ui.timeline.generated.resources.sign_in
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PostInteractions(
    replyCount: String?,
    repostCount: String?,
    likeCount: String?,
    repostUri: GenericUri?,
    likeUri: GenericUri?,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    postId: PostId,
    postUri: PostUri,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    modifier: Modifier = Modifier,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = presentation.postInteractionArrangement.animate(),
    ) {
        PostInteractionsButtons(
            replyCount = replyCount,
            repostCount = repostCount,
            likeCount = likeCount,
            repostUri = repostUri,
            likeUri = likeUri,
            isBookmarked = isBookmarked,
            isDownloaded = isDownloaded,
            postId = postId,
            postUri = postUri,
            sharedElementPrefix = sharedElementPrefix,
            iconSize = animateDpAsState(presentation.actionIconSize).value,
            orientation = Orientation.Horizontal,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            onReplyToPost = onReplyToPost,
            onPostInteraction = onPostInteraction,
        )
        Spacer(Modifier.width(0.dp))
    }
}

@Composable
fun MediaPostInteractions(
    post: Post,
    sharedElementPrefix: String,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    modifier: Modifier = Modifier,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PostInteractionsButtons(
            replyCount = format(post.replyCount),
            repostCount = format(post.repostCount),
            likeCount = format(post.likeCount),
            repostUri = post.viewerStats?.repostUri,
            likeUri = post.viewerStats?.likeUri,
            isBookmarked = post.viewerStats?.bookmarked ?: false,
            isDownloaded = false,
            postId = post.cid,
            postUri = post.uri,
            sharedElementPrefix = sharedElementPrefix,
            iconSize = 40.dp,
            orientation = Orientation.Vertical,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            onReplyToPost = onReplyToPost,
            onPostInteraction = onPostInteraction,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private inline fun PostInteractionsButtons(
    replyCount: String?,
    repostCount: String?,
    likeCount: String?,
    repostUri: GenericUri?,
    likeUri: GenericUri?,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    postId: PostId,
    postUri: PostUri,
    sharedElementPrefix: String,
    iconSize: Dp,
    orientation: Orientation,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    crossinline onReplyToPost: () -> Unit,
    crossinline onPostInteraction: (Post.Interaction) -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    PostInteractionButton.All.forEach { button ->
        key(button) {
            PostInteraction(
                modifier = Modifier
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = postActionSharedElementKey(
                                prefix = sharedElementPrefix,
                                postId = postId,
                                button = button,
                            ),
                        ),
                    ),
                icon = when (button) {
                    PostInteractionButton.Comment -> button.icon(isChecked = false)
                    PostInteractionButton.Like -> button.icon(isChecked = likeUri != null)
                    PostInteractionButton.Repost -> button.icon(isChecked = repostUri != null)
                    PostInteractionButton.Bookmark -> button.icon(isChecked = isBookmarked)
                    PostInteractionButton.Download -> button.icon(isChecked = !isDownloaded)
                },
                iconSize = iconSize,
                orientation = orientation,
                contentDescription = stringResource(button.stringResource),
                text = when (button) {
                    PostInteractionButton.Comment -> replyCount
                    PostInteractionButton.Like -> likeCount
                    PostInteractionButton.Repost -> repostCount
                    PostInteractionButton.Bookmark -> ""
                    PostInteractionButton.Download -> ""
                },
                tint = when (button) {
                    PostInteractionButton.Comment -> MaterialTheme.colorScheme.outline
                    PostInteractionButton.Like ->
                        if (likeUri != null) LikeRed
                        else MaterialTheme.colorScheme.outline

                    PostInteractionButton.Repost ->
                        if (repostUri != null) RepostGreen
                        else MaterialTheme.colorScheme.outline
                    PostInteractionButton.Bookmark ->
                        if (isBookmarked) BookmarkBlue
                        else MaterialTheme.colorScheme.outline
                    PostInteractionButton.Download ->
                        if (!isDownloaded) MaterialTheme.colorScheme.outline
                        else Color.Transparent
                },
                onClick = {
                    when (button) {
                        PostInteractionButton.Comment -> onReplyToPost()
                        PostInteractionButton.Like -> onPostInteraction(
                            when (likeUri) {
                                null -> Like(
                                    postId = postId,
                                    postUri = postUri,
                                )

                                else -> Unlike(
                                    postUri = postUri,
                                    likeUri = likeUri,
                                )
                            },
                        )

                        PostInteractionButton.Repost -> onPostInteraction(
                            when (repostUri) {
                                null -> Repost(
                                    postId = postId,
                                    postUri = postUri,
                                )

                                else -> RemoveRepost(
                                    postUri = postUri,
                                    repostUri = repostUri,
                                )
                            },
                        )
                        PostInteractionButton.Bookmark -> onPostInteraction(
                            when (isBookmarked) {
                                false -> Bookmark(
                                    postId = postId,
                                    postUri = postUri,
                                )

                                true -> RemoveBookmark(
                                    postUri = postUri,
                                )
                            },
                        )
                        PostInteractionButton.Download -> {

                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PostInteraction(
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
    orientation: Orientation,
    text: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.outline,
) {
    val itemModifier = modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false),
            onClick = onClick,
        )
        .padding(
            top = 4.dp,
            bottom = 2.dp,
        )

    when (orientation) {
        Orientation.Vertical -> Column(
            modifier = itemModifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PostInteractionElements(
                icon = icon,
                iconSize = iconSize,
                contentDescription = contentDescription,
                text = text,
                tint = tint,
            )
        }
        Orientation.Horizontal -> Row(
            modifier = itemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PostInteractionElements(
                icon = icon,
                iconSize = iconSize,
                contentDescription = contentDescription,
                text = text,
                tint = tint,
            )
        }
    }
}

@Composable
private fun PostInteractionElements(
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    text: String?,
    tint: Color,
) {
    Icon(
        modifier = Modifier.size(iconSize),
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        tint = tint,
    )

    if (text != null) {
        BasicText(
            modifier = Modifier
                .padding(vertical = 1.dp),
            text = text,
            maxLines = 1,
            color = { tint },
            autoSize = TextAutoSize.StepBased(
                minFontSize = 4.sp,
                maxFontSize = 16.sp,
            ),
        )
    }
}

@Stable
class PostInteractionsSheetState private constructor(
    isSignedIn: Boolean,
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {
    var currentInteraction by mutableStateOf<Post.Interaction?>(null)
        internal set

    var showBottomSheet by mutableStateOf(false)
        internal set

    internal var isSignedIn by mutableStateOf(isSignedIn)

    fun onInteraction(interaction: Post.Interaction) {
        currentInteraction = interaction
    }

    internal fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentInteraction = null
            }
        }
    }

    companion object {
        @Composable
        fun rememberUpdatedPostInteractionState(
            isSignedIn: Boolean,
            onSignInClicked: () -> Unit,
            onInteractionConfirmed: (Post.Interaction) -> Unit,
            onQuotePostClicked: (Post.Interaction.Create.Repost) -> Unit,
        ): PostInteractionsSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            val state = remember(sheetState, scope) {
                PostInteractionsSheetState(
                    isSignedIn = isSignedIn,
                    sheetState = sheetState,
                    scope = scope,
                )
            }.also { it.isSignedIn = isSignedIn }

            PostInteractionsBottomSheet(
                state = state,
                onSignInClicked = onSignInClicked,
                onInteractionConfirmed = onInteractionConfirmed,
                onQuotePostClicked = onQuotePostClicked,
            )

            return state
        }
    }
}

@Composable
private fun PostInteractionsBottomSheet(
    state: PostInteractionsSheetState,
    onSignInClicked: () -> Unit,
    onInteractionConfirmed: (Post.Interaction) -> Unit,
    onQuotePostClicked: (Post.Interaction.Create.Repost) -> Unit,
) {
    LaunchedEffect(state.currentInteraction) {
        when (val interaction = state.currentInteraction) {
            null -> Unit
            is Post.Interaction.Create.Repost -> state.showBottomSheet = true
            is Post.Interaction.Create.Like,
            is Post.Interaction.Delete.RemoveRepost,
            is Post.Interaction.Delete.Unlike,
            is Post.Interaction.Create.Bookmark,
            is Post.Interaction.Delete.RemoveBookmark,
            -> {
                if (state.isSignedIn) {
                    onInteractionConfirmed(interaction)
                    state.currentInteraction = null
                } else {
                    state.showBottomSheet = true
                }
            }
        }
    }

    if (state.showBottomSheet) ModalBottomSheet(
        onDismissRequest = {
            state.showBottomSheet = false
        },
        sheetState = state.sheetState,
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isSignedIn) repeat(2) { index ->
                    val contentDescription = stringResource(
                        if (index == 0) Res.string.repost
                        else Res.string.quote,
                    ).capitalize(locale = Locale.current)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable {
                                when (index) {
                                    0 ->
                                        state.currentInteraction
                                            ?.let(onInteractionConfirmed)

                                    else -> (state.currentInteraction as? Post.Interaction.Create.Repost)
                                        ?.let(onQuotePostClicked)
                                }
                                state.hideSheet()
                            }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp,
                            )
                            .semantics {
                                this.contentDescription = contentDescription
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                imageVector = if (index == 0) Icons.Rounded.Repeat
                                else Icons.Rounded.FormatQuote,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier,
                                text = contentDescription,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                    )
                }

                // Sheet content
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        if (!state.isSignedIn) onSignInClicked()
                        state.hideSheet()
                    },
                    content = {
                        Text(
                            text = stringResource(
                                if (state.isSignedIn) Res.string.cancel
                                else Res.string.sign_in,
                            )
                                .capitalize(Locale.current),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                )
                Spacer(
                    Modifier.height(16.dp),
                )
            }
        },
    )
}

private val LikeRed = Color(0xFFE0245E)
private val RepostGreen = Color(0xFF17BF63)
private val BookmarkBlue = Color(0xFF1D9BF0)

private val Timeline.Presentation.postInteractionArrangement: Arrangement.Horizontal
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> Arrangement.SpaceBetween
        Timeline.Presentation.Media.Expanded -> Arrangement.spacedBy(24.dp)
        Timeline.Presentation.Media.Condensed -> Arrangement.SpaceBetween
        Timeline.Presentation.Media.Grid -> Arrangement.SpaceBetween
    }

private val Timeline.Presentation.actionIconSize
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 16.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Expanded -> 24.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

private fun postActionSharedElementKey(
    prefix: String,
    postId: PostId,
    button: PostInteractionButton,
): String = "$prefix-${postId.id}-${button.hashCode()}"

private sealed class PostInteractionButton {

    data object Comment : PostInteractionButton()
    data object Repost : PostInteractionButton()
    data object Like : PostInteractionButton()
    data object Bookmark : PostInteractionButton()
    data object Download : PostInteractionButton()

    companion object {
        fun PostInteractionButton.icon(
            isChecked: Boolean,
        ) = when (this) {
            Comment -> Icons.Rounded.ChatBubbleOutline
            Like -> if (isChecked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
            Repost -> Icons.Rounded.Repeat
            Bookmark -> if (isChecked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder
            Download -> Icons.Rounded.Download
        }

        val PostInteractionButton.stringResource
            get() = when (this) {
                Comment -> Res.string.reply
                Like -> Res.string.liked
                Repost -> Res.string.repost
                Bookmark -> Res.string.bookmarked
                Download -> Res.string.download
            }

        val All = listOf(
            Comment,
            Repost,
            Like,
            Bookmark,
            Download
        )
    }
}
