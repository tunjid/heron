package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditAttributes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.timeline.utilities.BottomSheetItemCard
import com.tunjid.heron.timeline.utilities.BottomSheetItemCardRow
import com.tunjid.heron.timeline.utilities.CopyToClipboardCard
import com.tunjid.heron.timeline.utilities.SendDirectMessageCard
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

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
        fun rememberUpdatedPostOptionsState(
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
            state.currentPost?.let {
                if (it.author.did == signedInProfileId) BottomSheetItemCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        onOptionClicked(PostOption.ThreadGate(it.uri))
                    },
                    content = {
                        BottomSheetItemCardRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            icon = Icons.Rounded.EditAttributes,
                            text = "",
                        )
                    },
                )
                CopyToClipboardCard(it.uri.shareUri())
            }
        }
    }
}

sealed class PostOption {
    data class ShareInConversation(
        val post: Post,
        val conversation: Conversation,
    ) : PostOption()

    data class ThreadGate(
        val postUri: PostUri,
    ) : PostOption()
}
