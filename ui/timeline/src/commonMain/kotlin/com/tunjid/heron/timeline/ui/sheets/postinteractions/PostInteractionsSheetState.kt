package com.tunjid.heron.timeline.ui.sheets.postinteractions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.canQuote
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.mutator.compose.produceState
import heron.ui.core.generated.resources.cancel
import heron.ui.core.generated.resources.sign_in
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.quote
import heron.ui.timeline.generated.resources.remove_repost
import heron.ui.timeline.generated.resources.repost
import org.jetbrains.compose.resources.stringResource

@Stable
class PostInteractionsSheetState(
    scope: BottomSheetScope,
    internal val viewModel: PostInteractionsViewModel,
) : BottomSheetState(scope) {

    var showingAction by mutableStateOf<PostAction.OfInteraction?>(null)
        internal set

    val state: PostInteractionsState get() = viewModel.state

    fun onInteraction(interaction: PostAction.OfInteraction) {
        showingAction = interaction
    }

    override fun onHidden() {
        showingAction = null
    }

    companion object {
        @Composable
        fun rememberUpdatedPostInteractionsSheetState(
            initializer: PostInteractionsViewModelInitializer,
            onSignInClicked: () -> Unit,
            onInteractionConfirmed: (Post.Interaction) -> Unit,
            onQuotePostClicked: (Post.Interaction.Create.Repost) -> Unit,
        ): PostInteractionsSheetState {
            val state = rememberBottomSheetState(
                viewModelInitializer = initializer::invoke,
                block = ::PostInteractionsSheetState,
            )

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
            is Post.Interaction.Create.Repost,
            is Post.Interaction.Delete.RemoveRepost,
            -> state.show()
            is Post.Interaction.Create.Like,
            is Post.Interaction.Delete.Unlike,
            is Post.Interaction.Create.Bookmark,
            is Post.Interaction.Delete.RemoveBookmark,
            is Post.Interaction.Upsert.Gate,
            -> {
                if (state.state.isSignedIn) {
                    onInteractionConfirmed(interaction)
                    state.showingAction = null
                } else {
                    state.show()
                }
            }
        }
    }

    state.ModalBottomSheet {
        val postInteractionsState = state.viewModel.produceState()
        val action = state.showingAction
        val currentInteraction = action?.interaction

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (postInteractionsState.isSignedIn) {
                when (currentInteraction) {
                    is Post.Interaction.Create.Repost,
                    is Post.Interaction.Delete.RemoveRepost,
                    -> {
                        Item(
                            contentDescription = stringResource(
                                if (currentInteraction is Post.Interaction.Create.Repost)
                                    Res.string.repost
                                else Res.string.remove_repost,
                            ),
                            enabled = true,
                            icon = Icons.Rounded.Repeat,
                            onClick = {
                                onInteractionConfirmed(currentInteraction)
                                state.hide()
                            },
                        )
                        Item(
                            contentDescription = stringResource(Res.string.quote),
                            enabled = action.viewerStats.canQuote,
                            icon = Icons.Rounded.FormatQuote,
                            onClick = {
                                onQuotePostClicked(
                                    Post.Interaction.Create.Repost(
                                        postUri = currentInteraction.postUri,
                                        postId = if (currentInteraction is Post.Interaction.Create.Repost)
                                            currentInteraction.postId
                                        else PostId(""),
                                    ),
                                )
                                state.hide()
                            },
                        )
                    }
                    else -> Unit
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!postInteractionsState.isSignedIn) onSignInClicked()
                    state.hide()
                },
                content = {
                    Text(
                        text = stringResource(
                            if (postInteractionsState.isSignedIn) CommonStrings.cancel
                            else CommonStrings.sign_in,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Item(
    contentDescription: String,
    enabled: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .then(
                when {
                    enabled -> Modifier.clickable(onClick = onClick)
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
                imageVector = icon,
                contentDescription = null,
            )
            Text(
                modifier = Modifier,
                text = contentDescription
                    .capitalize(locale = Locale.current),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    )
}
