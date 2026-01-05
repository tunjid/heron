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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.canQuote
import com.tunjid.heron.data.core.models.canReply
import com.tunjid.heron.data.core.models.isBookmarked
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.icon
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.stringResource
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.UiTokens.BookmarkBlue
import com.tunjid.heron.ui.UiTokens.LikeRed
import com.tunjid.heron.ui.UiTokens.RepostGreen
import com.tunjid.heron.ui.UiTokens.withDim
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.bookmarked
import heron.ui.timeline.generated.resources.cancel
import heron.ui.timeline.generated.resources.expand_options
import heron.ui.timeline.generated.resources.liked
import heron.ui.timeline.generated.resources.quote
import heron.ui.timeline.generated.resources.reply
import heron.ui.timeline.generated.resources.repost
import heron.ui.timeline.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource

@Composable
fun PostInteractions(
    post: Post,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    modifier: Modifier = Modifier,
    onInteraction: (PostAction.Options) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = presentation.postInteractionArrangement.animate(),
    ) {
        PostInteractionsButtons(
            interactionButtons = PostInteractionButton.PostButtons,
            post = post,
            sharedElementPrefix = sharedElementPrefix,
            iconSize = animateDpAsState(presentation.actionIconSize).value,
            orientation = Orientation.Horizontal,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            onInteraction = onInteraction,
            prefixContent = spacer@{ button ->
                if (button != PostInteractionButton.MoreOptions) return@spacer
                if (presentation != Timeline.Presentation.Media.Expanded) return@spacer

                Spacer(Modifier.weight(1f))
            },
        )
    }
}

@Composable
fun MediaPostInteractions(
    post: Post,
    sharedElementPrefix: String,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    modifier: Modifier = Modifier,
    onInteraction: (PostAction.Options) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PostInteractionsButtons(
            interactionButtons = PostInteractionButton.MediaButtons,
            post = post,
            sharedElementPrefix = sharedElementPrefix,
            iconSize = 40.dp,
            orientation = Orientation.Vertical,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            onInteraction = onInteraction,
        )
    }
}

@Composable
private inline fun PostInteractionsButtons(
    interactionButtons: List<PostInteractionButton>,
    post: Post,
    sharedElementPrefix: String,
    iconSize: Dp,
    orientation: Orientation,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    crossinline onInteraction: (PostAction.Options) -> Unit,
    crossinline prefixContent: @Composable (PostInteractionButton) -> Unit = {},
) = with(paneMovableElementSharedTransitionScope) {
    interactionButtons.forEach { button ->
        key("$button-prefix") {
            prefixContent(button)
        }
        key(button) {
            PaneStickySharedElement(
                modifier = Modifier,
                sharedContentState = rememberSharedContentState(
                    key = postActionSharedElementKey(
                        prefix = sharedElementPrefix,
                        postId = post.cid,
                        button = button,
                    ),
                ),
            ) {
                PostInteraction(
                    modifier = Modifier,
                    icon = when (button) {
                        PostInteractionButton.Comment -> button.icon(isChecked = false)
                        PostInteractionButton.Like -> button.icon(isChecked = post.viewerStats?.likeUri != null)
                        PostInteractionButton.Repost -> button.icon(isChecked = post.viewerStats?.repostUri != null)
                        PostInteractionButton.Bookmark -> button.icon(isChecked = post.viewerStats.isBookmarked)
                        PostInteractionButton.MoreOptions -> button.icon(isChecked = false)
                    },
                    iconSize = iconSize,
                    orientation = orientation,
                    contentDescription = stringResource(button.stringResource),
                    text = when (button) {
                        PostInteractionButton.Comment ->
                            if (post.replyCount > 0) format(post.replyCount)
                            else ""
                        PostInteractionButton.Like ->
                            if (post.likeCount > 0) format(post.likeCount)
                            else ""
                        PostInteractionButton.Repost ->
                            if (post.repostCount > 0) format(post.repostCount)
                            else ""
                        PostInteractionButton.Bookmark -> ""
                        PostInteractionButton.MoreOptions -> ""
                    },
                    tint = animateColorAsState(
                        targetValue = when (button) {
                            PostInteractionButton.Comment -> MaterialTheme.colorScheme.outline
                            PostInteractionButton.Like ->
                                if (post.viewerStats?.likeUri != null) LikeRed
                                else MaterialTheme.colorScheme.outline

                            PostInteractionButton.Repost ->
                                if (post.viewerStats?.repostUri != null) RepostGreen
                                else MaterialTheme.colorScheme.outline
                            PostInteractionButton.Bookmark ->
                                if (post.viewerStats.isBookmarked) BookmarkBlue
                                else MaterialTheme.colorScheme.outline
                            PostInteractionButton.MoreOptions -> MaterialTheme.colorScheme.outline
                        },
                        animationSpec = ColorTween,
                    ).value,
                    enabled = when (button) {
                        PostInteractionButton.Bookmark -> true
                        PostInteractionButton.Comment -> post.viewerStats.canReply
                        PostInteractionButton.Like -> true
                        PostInteractionButton.MoreOptions -> true
                        PostInteractionButton.Repost -> true
                    },
                    onClick = {
                        when (button) {
                            PostInteractionButton.Comment -> onInteraction(
                                PostAction.OfReply(post),
                            )
                            PostInteractionButton.Like -> onInteraction(
                                PostAction.OfInteraction(
                                    interaction = when (val likeUri = post.viewerStats?.likeUri) {
                                        null -> Post.Interaction.Create.Like(
                                            postId = post.cid,
                                            postUri = post.uri,
                                        )

                                        else -> Post.Interaction.Delete.Unlike(
                                            postUri = post.uri,
                                            likeUri = likeUri,
                                        )
                                    },
                                    viewerStats = post.viewerStats,
                                ),
                            )

                            PostInteractionButton.Repost -> onInteraction(
                                PostAction.OfInteraction(
                                    when (val repostUri = post.viewerStats?.repostUri) {
                                        null -> Post.Interaction.Create.Repost(
                                            postId = post.cid,
                                            postUri = post.uri,
                                        )

                                        else -> Post.Interaction.Delete.RemoveRepost(
                                            postUri = post.uri,
                                            repostUri = repostUri,
                                        )
                                    },
                                    post.viewerStats,
                                ),
                            )
                            PostInteractionButton.Bookmark -> onInteraction(
                                PostAction.OfInteraction(
                                    when (post.viewerStats.isBookmarked) {
                                        false -> Post.Interaction.Create.Bookmark(
                                            postId = post.cid,
                                            postUri = post.uri,
                                        )

                                        true -> Post.Interaction.Delete.RemoveBookmark(
                                            postUri = post.uri,
                                        )
                                    },
                                    post.viewerStats,
                                ),
                            )
                            PostInteractionButton.MoreOptions -> onInteraction(
                                PostAction.OfMore(post),
                            )
                        }
                    },
                )
            }
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
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color,
) {
    val itemModifier = modifier
        .then(
            if (enabled) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            else Modifier,
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
                tint = tint.withDim(!enabled),
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
                tint = tint.withDim(!enabled),
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
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    internal var showingAction by mutableStateOf<PostAction.OfInteraction?>(null)

    internal var isSignedIn by mutableStateOf(isSignedIn)

    fun onInteraction(
        interaction: PostAction.OfInteraction,
    ) {
        showingAction = interaction
    }

    override fun onHidden() {
        showingAction = null
    }

    companion object {
        @Composable
        fun rememberUpdatedPostInteractionsSheetState(
            isSignedIn: Boolean,
            onSignInClicked: () -> Unit,
            onInteractionConfirmed: (Post.Interaction) -> Unit,
            onQuotePostClicked: (Post.Interaction.Create.Repost) -> Unit,
        ): PostInteractionsSheetState {
            val state = rememberBottomSheetState {
                PostInteractionsSheetState(
                    isSignedIn = isSignedIn,
                    scope = it,
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
    LaunchedEffect(state.showingAction) {
        when (val interaction = state.showingAction?.interaction) {
            null -> Unit
            is Post.Interaction.Create.Repost -> state.show()
            is Post.Interaction.Create.Like,
            is Post.Interaction.Delete.RemoveRepost,
            is Post.Interaction.Delete.Unlike,
            is Post.Interaction.Create.Bookmark,
            is Post.Interaction.Delete.RemoveBookmark,
            is Post.Interaction.Upsert.Gate,
            -> {
                if (state.isSignedIn) {
                    onInteractionConfirmed(interaction)
                    state.showingAction = null
                } else {
                    state.show()
                }
            }
        }
    }

    state.ModalBottomSheet {
        val action = state.showingAction
        val currentInteraction = action?.interaction
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (currentInteraction) {
                is Post.Interaction.Create.Repost -> {
                    if (state.isSignedIn) repeat(2) { index ->
                        val isRepost = index == 0
                        val contentDescription = stringResource(
                            if (isRepost) Res.string.repost
                            else Res.string.quote,
                        ).capitalize(locale = Locale.current)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .then(
                                    when {
                                        isRepost -> Modifier.clickable {
                                            onInteractionConfirmed(currentInteraction)
                                            state.hide()
                                        }
                                        action.viewerStats.canQuote -> Modifier.clickable {
                                            onQuotePostClicked(currentInteraction)
                                            state.hide()
                                        }
                                        else -> Modifier.alpha(0.6f)
                                    },
                                )
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
                                    imageVector =
                                    if (isRepost) Icons.Rounded.Repeat
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
                }
                else -> Unit
            }

            // Sheet content
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = {
                    if (!state.isSignedIn) onSignInClicked()
                    state.hide()
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
    }
}

private val ColorTween = tween<Color>(durationMillis = 100)

private val Timeline.Presentation.postInteractionArrangement: Arrangement.Horizontal
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> Arrangement.SpaceBetween
        Timeline.Presentation.Media.Expanded -> Arrangement.spacedBy(32.dp)
        Timeline.Presentation.Media.Condensed -> Arrangement.SpaceBetween
        Timeline.Presentation.Media.Grid -> Arrangement.SpaceBetween
    }

private val Timeline.Presentation.actionIconSize
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 18.dp
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
    data object MoreOptions : PostInteractionButton()

    companion object {
        fun PostInteractionButton.icon(
            isChecked: Boolean,
        ) = when (this) {
            Comment -> Icons.Rounded.ChatBubbleOutline
            Like -> if (isChecked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
            Repost -> Icons.Rounded.Repeat
            Bookmark -> if (isChecked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder
            MoreOptions -> Icons.Rounded.ArrowCircleUp
        }

        val PostInteractionButton.stringResource
            get() = when (this) {
                Comment -> Res.string.reply
                Like -> Res.string.liked
                Repost -> Res.string.repost
                Bookmark -> Res.string.bookmarked
                MoreOptions -> Res.string.expand_options
            }

        val PostButtons = listOf(
            Comment,
            Repost,
            Like,
            Bookmark,
            MoreOptions,
        )

        val MediaButtons = listOf(
            Like,
            Comment,
            Repost,
            Bookmark,
            MoreOptions,
        )
    }
}
