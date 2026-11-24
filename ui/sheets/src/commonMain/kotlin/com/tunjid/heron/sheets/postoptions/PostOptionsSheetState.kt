package com.tunjid.heron.sheets.postoptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.RecordPostReference
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.types.PostReferencingRecordUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.sheets.utilities.BottomSheetItemCard
import com.tunjid.heron.sheets.utilities.BottomSheetItemCardRow
import com.tunjid.heron.sheets.utilities.CopyToClipboardCard
import com.tunjid.heron.sheets.utilities.ItemHorizontalDivider
import com.tunjid.heron.sheets.utilities.SendDirectMessageCard
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.VolumeOff
import com.tunjid.heron.ui.icons.regular.Delete
import com.tunjid.heron.ui.icons.regular.EditAttributes
import com.tunjid.heron.ui.icons.regular.FilterAlt
import com.tunjid.heron.ui.icons.regular.Link
import com.tunjid.heron.ui.icons.regular.LinkOff
import com.tunjid.heron.ui.icons.regular.PersonOff
import com.tunjid.heron.ui.icons.regular.ThumbDown
import com.tunjid.heron.ui.icons.regular.ThumbUp
import com.tunjid.heron.ui.rememberSimpleDialogState
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.compose.produceState
import heron.ui.core.generated.resources.no
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.core.generated.resources.yes
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.attach_post_to_event
import heron.ui.timeline.generated.resources.attach_post_to_event_overwrite_prompt
import heron.ui.timeline.generated.resources.delete_post
import heron.ui.timeline.generated.resources.delete_post_prompt
import heron.ui.timeline.generated.resources.detach_post_from_event
import heron.ui.timeline.generated.resources.feed_interaction_show_less
import heron.ui.timeline.generated.resources.feed_interaction_show_more
import heron.ui.timeline.generated.resources.link_document_to_post
import heron.ui.timeline.generated.resources.link_document_to_post_overwrite_prompt
import heron.ui.timeline.generated.resources.mute_words_tags
import heron.ui.timeline.generated.resources.thread_gate_post_reply_settings
import heron.ui.timeline.generated.resources.unlink_document_from_post
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class PostOptionsSheetState(
    scope: BottomSheetScope,
    internal val stateHolder: PostOptionsStateHolder,
) : BottomSheetState(scope) {

    var currentPost: Post? by mutableStateOf(null)
        internal set

    var interactionContext: FeedGenerator.Interaction.Context? by mutableStateOf(null)
        internal set

    val messages: List<Memo> get() = stateHolder.state.messages

    override fun onHidden() {
        currentPost = null
        interactionContext = null
    }

    fun showOptions(
        post: Post,
        feedInteraction: FeedGenerator.Interaction.Context? = null,
    ) {
        currentPost = post
        this.interactionContext = feedInteraction
        show()
    }

    fun onSnackbarMessageConsumed(memo: Memo) {
        stateHolder.accept(PostOptionsAction.SnackbarDismissed(memo))
    }

    companion object {
        @Composable
        fun rememberUpdatedPostOptionsSheetState(
            stateHolder: PostOptionsStateHolder,
            onOptionClicked: (PostOption) -> Unit,
        ): PostOptionsSheetState {
            val state = rememberBottomSheetState(
                stateHolder = stateHolder,
                block = ::PostOptionsSheetState,
            )

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
    state.ModalBottomSheet {
        val postOptionsState = state.stateHolder.produceState()

        val signedInProfileId = postOptionsState.signedInProfileId
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (signedInProfileId != null) SendDirectMessageCard(
                signedInProfileId = signedInProfileId,
                recentConversations = postOptionsState.recentConversations,
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

                state.interactionContext?.let { feedInteraction ->
                    FeedInteractionMenuSection(
                        post = post,
                        interactionContext = feedInteraction,
                        state = state,
                    )
                }

                if (signedInProfileId != null && !isOwnPost) PostModerationMenuSection(
                    signedInProfileId = signedInProfileId,
                    post = post,
                    onOptionClicked = { option ->
                        state.hide()
                        onOptionClicked(option)
                    },
                )

                if (isOwnPost) PostManagementMenuSection(
                    state = state,
                    signedInProfileId = signedInProfileId,
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
private fun FeedInteractionMenuSection(
    modifier: Modifier = Modifier,
    post: Post,
    interactionContext: FeedGenerator.Interaction.Context,
    state: PostOptionsSheetState,
) {
    BottomSheetItemCard(modifier) {
        BottomSheetItemCardRow(
            modifier = Modifier
                .fillMaxWidth(),
            icon = HeronIcons.Regular.ThumbUp,
            text = stringResource(Res.string.feed_interaction_show_more),
            onClick = {
                state.sendFeedInteraction(
                    post = post,
                    feedInteraction = interactionContext,
                    event = FeedGenerator.Interaction.Event.Request.More,
                )
            },
        )
        ItemHorizontalDivider()
        BottomSheetItemCardRow(
            modifier = Modifier
                .fillMaxWidth(),
            icon = HeronIcons.Regular.ThumbDown,
            text = stringResource(Res.string.feed_interaction_show_less),
            onClick = {
                state.sendFeedInteraction(
                    post = post,
                    feedInteraction = interactionContext,
                    event = FeedGenerator.Interaction.Event.Request.Less,
                )
            },
        )
    }
}

private fun PostOptionsSheetState.sendFeedInteraction(
    post: Post,
    feedInteraction: FeedGenerator.Interaction.Context,
    event: FeedGenerator.Interaction.Event.Request,
) {
    stateHolder.accept(
        PostOptionsAction.SendFeedInteraction(
            request = FeedGenerator.Interaction.Request(
                feedUri = feedInteraction.feedUri,
                postUri = post.uri,
                event = event,
                feedContext = feedInteraction.feedContext,
                reqId = feedInteraction.reqId,
            ),
        ),
    )
    hide()
}

@Composable
private fun PostManagementMenuSection(
    modifier: Modifier = Modifier,
    state: PostOptionsSheetState,
    signedInProfileId: ProfileId?,
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
                icon = HeronIcons.Regular.EditAttributes,
                text = stringResource(Res.string.thread_gate_post_reply_settings),
                onClick = {
                    onOptionClicked(PostOption.ThreadGate(post.uri))
                },
            )
            ItemHorizontalDivider()

            // Only the owner of both the post and a record it embeds can attach the post to
            // that record. Show "detach" when the record already points here, otherwise
            // "attach" (with a confirm when it currently points at a different post).
            val ownRecord = post.embeddedRecords.firstNotNullOfOrNull { record ->
                record.asOwnPostReferencingRecordOrNull(signedInProfileId)
            }
            if (ownRecord != null) {
                val overwriteDialogState = rememberSimpleDialogState()
                val isLinkedPost = ownRecord.linkedPostUri == post.uri
                val dispatch = {
                    state.stateHolder.accept(
                        PostOptionsAction.UpdatePostReference(
                            if (isLinkedPost) RecordPostReference.Unlink(
                                recordUri = ownRecord.uri,
                            )
                            else RecordPostReference.Link(
                                recordUri = ownRecord.uri,
                                postUri = post.uri,
                                postCid = post.cid,
                            ),
                        ),
                    )
                    state.hide()
                }

                BottomSheetItemCardRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    icon =
                    if (isLinkedPost) HeronIcons.Regular.LinkOff
                    else HeronIcons.Regular.Link,
                    text = stringResource(
                        if (isLinkedPost) ownRecord.detachText
                        else ownRecord.attachText,
                    ),
                    onClick = {
                        when {
                            !isLinkedPost && ownRecord.linkedPostUri != null -> overwriteDialogState.show()
                            else -> dispatch()
                        }
                    },
                )

                SimpleDialog(
                    state = overwriteDialogState,
                    title = {
                        SimpleDialogTitle(
                            text = stringResource(ownRecord.attachText),
                        )
                    },
                    text = {
                        SimpleDialogText(
                            text = stringResource(ownRecord.overwritePrompt),
                        )
                    },
                    confirmButton = {
                        DestructiveDialogButton(
                            text = stringResource(CommonStrings.yes),
                            onClick = {
                                overwriteDialogState.hide()
                                dispatch()
                            },
                        )
                    },
                    dismissButton = {
                        NeutralDialogButton(
                            text = stringResource(CommonStrings.no),
                            onClick = overwriteDialogState::hide,
                        )
                    },
                )

                ItemHorizontalDivider()
            }

            val deletePostDialogState = rememberSimpleDialogState()
            BottomSheetItemCardRow(
                modifier = Modifier
                    .fillMaxWidth(),
                icon = HeronIcons.Regular.Delete,
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
        icon = HeronIcons.Regular.FilterAlt,
    ),
    BlockAccount(
        stringRes = CommonStrings.viewer_state_block_account,
        icon = HeronIcons.Regular.PersonOff,
    ),
    MuteAccount(
        stringRes = CommonStrings.viewer_state_mute_account,
        icon = HeronIcons.AutoMirrored.VolumeOff,
    ),
}

/**
 * A record the signed-in user owns that a post can be attached to.
 */
private class OwnPostReferencingRecord(
    val uri: PostReferencingRecordUri,
    val linkedPostUri: RecordUri?,
    val attachText: StringResource,
    val detachText: StringResource,
    val overwritePrompt: StringResource,
)

private fun Record.Embeddable.asOwnPostReferencingRecordOrNull(
    signedInProfileId: ProfileId?,
): OwnPostReferencingRecord? = when (this) {
    is StandardDocument -> takeIf { it.authorId == signedInProfileId }?.let { document ->
        OwnPostReferencingRecord(
            uri = document.uri,
            linkedPostUri = document.bskyPostRef?.uri,
            attachText = Res.string.link_document_to_post,
            detachText = Res.string.unlink_document_from_post,
            overwritePrompt = Res.string.link_document_to_post_overwrite_prompt,
        )
    }
    is Event -> takeIf { it.author.did == signedInProfileId }?.let { event ->
        OwnPostReferencingRecord(
            uri = event.uri,
            linkedPostUri = event.bskyPostRef?.uri,
            attachText = Res.string.attach_post_to_event,
            detachText = Res.string.detach_post_from_event,
            overwritePrompt = Res.string.attach_post_to_event_overwrite_prompt,
        )
    }
    else -> null
}
