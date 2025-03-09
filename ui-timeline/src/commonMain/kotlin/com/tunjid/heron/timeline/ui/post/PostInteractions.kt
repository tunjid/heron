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
import androidx.compose.material.icons.rounded.ChatBubbleOutline
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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.icon
import com.tunjid.heron.timeline.ui.post.PostInteractionButton.Companion.stringResource
import com.tunjid.heron.timeline.utilities.actionIconSize
import com.tunjid.heron.ui.PanedSharedElementScope
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.cancel
import heron.ui_timeline.generated.resources.liked
import heron.ui_timeline.generated.resources.quote
import heron.ui_timeline.generated.resources.reply
import heron.ui_timeline.generated.resources.repost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.tunjid.composables.ui.animate


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostInteractions(
    replyCount: String?,
    repostCount: String?,
    likeCount: String?,
    repostUri: Uri?,
    likeUri: Uri?,
    postId: Id,
    postUri: Uri,
    sharedElementPrefix: String,
    presentation: Timeline.Presentation,
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) = with(panedSharedElementScope) {
    val arrangement: Arrangement.Horizontal = when (presentation) {
        Timeline.Presentation.Text.WithEmbed -> Arrangement.SpaceBetween
        Timeline.Presentation.Media.Expanded -> Arrangement.spacedBy(24.dp)
        Timeline.Presentation.Media.Condensed -> Arrangement.SpaceBetween
    }
    val iconSize = animateDpAsState(presentation.actionIconSize).value
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = arrangement.animate(),
    ) {
        PostInteractionButton.All.forEach { button ->
            PostInteraction(
                modifier = Modifier
                    .sharedElement(
                        key = postActionSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            button = button,
                        ),
                    ),
                icon = when (button) {
                    PostInteractionButton.Comment -> button.icon(isChecked = false)
                    PostInteractionButton.Like -> button.icon(isChecked = likeUri != null)
                    PostInteractionButton.Repost -> button.icon(isChecked = repostUri != null)
                },
                iconSize = iconSize,
                contentDescription = stringResource(button.stringResource),
                text = when (button) {
                    PostInteractionButton.Comment -> replyCount
                    PostInteractionButton.Like -> likeCount
                    PostInteractionButton.Repost -> repostCount
                },
                tint = when (button) {
                    PostInteractionButton.Comment -> MaterialTheme.colorScheme.outline
                    PostInteractionButton.Like -> if (likeUri != null) {
                        Color.Green
                    } else {
                        MaterialTheme.colorScheme.outline
                    }

                    PostInteractionButton.Repost -> if (repostUri != null) {
                        Color.Green
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                },
                onClick = {
                    when (button) {
                        PostInteractionButton.Comment -> onReplyToPost()
                        PostInteractionButton.Like -> onPostInteraction(
                            when (likeUri) {
                                null -> Post.Interaction.Create.Like(
                                    postId = postId,
                                    postUri = postUri,
                                )

                                else -> Post.Interaction.Delete.Unlike(
                                    postId = postId,
                                    likeUri = likeUri,
                                )
                            }
                        )

                        PostInteractionButton.Repost -> onPostInteraction(
                            when (repostUri) {
                                null -> Post.Interaction.Create.Repost(
                                    postId = postId,
                                    postUri = postUri,
                                )

                                else -> Post.Interaction.Delete.RemoveRepost(
                                    postId = postId,
                                    repostUri = repostUri,
                                )
                            }
                        )
                    }
                },
            )
        }
        Spacer(Modifier.width(0.dp))
    }
}

@Composable
private fun PostInteraction(
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
    text: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.outline,
) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .padding(
                top = 4.dp,
                bottom = 2.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
}

@Stable
class PostInteractionsSheetState private constructor(
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {
    var currentInteraction by mutableStateOf<Post.Interaction?>(null)
        internal set

    var showBottomSheet by mutableStateOf(false)
        internal set

    fun onInteraction(interaction: Post.Interaction) {
        currentInteraction = interaction
    }

    fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentInteraction = null
            }
        }
    }

    companion object {
        @Composable
        fun rememberPostInteractionState(): PostInteractionsSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            return remember {
                PostInteractionsSheetState(
                    sheetState = sheetState,
                    scope = scope,
                )
            }
        }
    }
}

@Composable
fun PostInteractionsBottomSheet(
    state: PostInteractionsSheetState,
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
                -> {
                onInteractionConfirmed(interaction)
                state.currentInteraction = null
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) { index ->

                    val contentDescription = stringResource(
                        if (index == 0) Res.string.repost
                        else Res.string.quote
                    ).capitalize(locale = Locale.current)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable {
                                if (index == 0) state.currentInteraction
                                    ?.let(onInteractionConfirmed)
                                else (state.currentInteraction as? Post.Interaction.Create.Repost)
                                    ?.let(onQuotePostClicked)
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
                                contentDescription = null
                            )
                            Text(
                                modifier = Modifier,
                                text = contentDescription,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }

                // Sheet content
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = state::hideSheet,
                    content = {
                        Text(
                            text = stringResource(Res.string.cancel)
                                .capitalize(Locale.current),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
                Spacer(
                    Modifier.height(16.dp)
                )
            }
        }
    )
}

private fun postActionSharedElementKey(
    prefix: String,
    postId: Id,
    button: PostInteractionButton,
): String = "$prefix-${postId.id}-${button.hashCode()}"

private sealed class PostInteractionButton {

    data object Comment : PostInteractionButton()
    data object Repost : PostInteractionButton()
    data object Like : PostInteractionButton()

    companion object {
        fun PostInteractionButton.icon(
            isChecked: Boolean,
        ) = when (this) {
            Comment -> Icons.Rounded.ChatBubbleOutline
            Like -> if (isChecked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
            Repost -> Icons.Rounded.Repeat
        }

        val PostInteractionButton.stringResource
            get() = when (this) {
                Comment -> Res.string.reply
                Like -> Res.string.liked
                Repost -> Res.string.repost
            }

        val All = listOf(
            Comment,
            Repost,
            Like,
        )
    }
}