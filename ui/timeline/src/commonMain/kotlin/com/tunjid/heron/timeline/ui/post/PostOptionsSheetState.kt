package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditAttributes
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.timeline.utilities.BottomSheetItemCard
import com.tunjid.heron.timeline.utilities.BottomSheetItemCardRow
import com.tunjid.heron.timeline.utilities.CopyToClipboardCard
import com.tunjid.heron.timeline.utilities.ItemHorizontalDivider
import com.tunjid.heron.timeline.utilities.SendDirectMessageCard
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.rememberSimpleDialogState
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.no
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.core.generated.resources.yes
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.delete_post
import heron.ui.timeline.generated.resources.delete_post_prompt
import heron.ui.timeline.generated.resources.mute_words_tags
import heron.ui.timeline.generated.resources.thread_gate_post_reply_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class PostOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentPost: Post? by mutableStateOf(null)

    internal val isSignedIn
        get() = signedInProfileId != null

    override fun onHidden() {
        currentPost = null
    }

    fun showOptions(post: Post) {
        currentPost = post
        show()
    }

    companion object {
        @Composable
        fun rememberUpdatedPostOptionsSheetState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onOptionClicked: (PostOption) -> Unit,
        ): PostOptionsSheetState {
            val state = rememberBottomSheetState {
                PostOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    scope = it,
                )
            }.also {
                it.signedInProfileId = signedInProfileId
                it.recentConversations = recentConversations
            }

            PostOptionsBottomSheet(
                state = state,
                onOptionClicked = onOptionClicked,
            )

            return state
        }
    }
}

@Composable
private fun PostOptionsBottomSheet(
    state: PostOptionsSheetState,
    onOptionClicked: (PostOption) -> Unit,
) {
    val signedInProfileId = state.signedInProfileId
    if (signedInProfileId != null) state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SendDirectMessageCard(
                signedInProfileId = signedInProfileId,
                recentConversations = state.recentConversations,
                onConversationClicked = { conversation ->
                    val currentPost = state.currentPost
                    if (currentPost != null) {
                        onOptionClicked(
                            PostOption.ShareInConversation(
                                post = currentPost,
                                conversation = conversation,
                            ),
                        )
                    }
                    state.hide()
                },
            )
            state.currentPost?.let { post ->
                val isOwnPost = post.author.did == signedInProfileId

                CopyToClipboardCard(post.uri.shareUri())

                if (!isOwnPost) PostModerationMenuSection(
                    signedInProfileId = signedInProfileId,
                    post = post,
                    onOptionClicked = { option ->
                        state.hide()
                        onOptionClicked(option)
                    },
                )

                if (isOwnPost) PostManagementMenuSection(
                    state = state,
                    post = post,
                    onOptionClicked = onOptionClicked,
                )
            }
        }
    }
}

@Composable
internal fun PostModerationMenuSection(
    modifier: Modifier = Modifier,
    signedInProfileId: ProfileId,
    post: Post,
    onOptionClicked: (PostOption) -> Unit,
) {
    BottomSheetItemCard(modifier) {
        PostModerationTools.entries.forEachIndexed { index, tool ->
            val isLast = index == PostModerationTools.entries.lastIndex

            BottomSheetItemCardRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = tool.icon,
                text = stringResource(tool.stringRes),
                onClick = {
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
                },
            )
            if (!isLast) {
                ItemHorizontalDivider()
            }
        }
    }
}

@Composable
private fun PostManagementMenuSection(
    modifier: Modifier = Modifier,
    state: PostOptionsSheetState,
    post: Post,
    onOptionClicked: (PostOption) -> Unit,
) {
    BottomSheetItemCard(
        modifier = modifier
            .fillMaxWidth(),
        content = {
            BottomSheetItemCardRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = Icons.Rounded.EditAttributes,
                text = stringResource(Res.string.thread_gate_post_reply_settings),
                onClick = {
                    onOptionClicked(PostOption.ThreadGate(post.uri))
                },
            )
            ItemHorizontalDivider()

            val deletePostDialogState = rememberSimpleDialogState()
            BottomSheetItemCardRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = Icons.Rounded.Delete,
                text = stringResource(Res.string.delete_post),
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    deletePostDialogState.show()
                },
            )

            SimpleDialog(
                state = deletePostDialogState,
                title = {
                    SimpleDialogTitle(
                        text = stringResource(Res.string.delete_post),
                    )
                },
                text = {
                    SimpleDialogText(
                        text = stringResource(Res.string.delete_post_prompt),
                    )
                },
                confirmButton = {
                    DestructiveDialogButton(
                        text = stringResource(CommonStrings.yes),
                        onClick = {
                            onOptionClicked(PostOption.Delete(post.uri))
                            deletePostDialogState.hide()
                            state.hide()
                        },
                    )
                },
                dismissButton = {
                    NeutralDialogButton(
                        text = stringResource(CommonStrings.no),
                        onClick = deletePostDialogState::hide,
                    )
                },
            )
        },
    )
}

sealed class PostOption {
    data class ShareInConversation(
        val post: Post,
        val conversation: Conversation,
    ) : PostOption()

    data class ThreadGate(
        val postUri: PostUri,
    ) : PostOption()

    data class Delete(
        val postUri: PostUri,
    ) : PostOption()

    sealed class Moderation : PostOption() {

        data object MuteWords : Moderation()

        sealed interface ProfileRestriction

        data class BlockAccount(
            val signedInProfileId: ProfileId,
            val post: Post,
        ) : Moderation(),
            ProfileRestriction

        data class MuteAccount(
            val signedInProfileId: ProfileId,
            val post: Post,
        ) : Moderation(),
            ProfileRestriction
    }
}

enum class PostModerationTools(
    val stringRes: StringResource,
    val icon: ImageVector,
) {
    MuteWords(
        stringRes = Res.string.mute_words_tags,
        icon = Icons.Rounded.FilterAlt,
    ),
    BlockAccount(
        stringRes = CommonStrings.viewer_state_block_account,
        icon = Icons.Rounded.PersonOff,
    ),
    MuteAccount(
        stringRes = CommonStrings.viewer_state_mute_account,
        icon = Icons.AutoMirrored.Rounded.VolumeOff,
    ),
}
