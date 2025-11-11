package com.tunjid.heron.timeline.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.timeline.ui.post.CopyToClipboardCard
import com.tunjid.heron.timeline.ui.post.SendDirectMessageCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class RecordOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {

    var showBottomSheet by mutableStateOf(false)
        internal set

    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentRecordUri: RecordUri? by mutableStateOf(null)

    internal val isSignedIn get() = signedInProfileId != null

    internal fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentRecordUri = null
            }
        }
    }

    fun showOptions(recordUri: RecordUri) {
        currentRecordUri = recordUri
        showBottomSheet = true
    }

    companion object {
        @Composable
        fun rememberUpdatedRecordOptionsState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (RecordUri, Conversation) -> Unit,
        ): RecordOptionsSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            val state = remember(sheetState, scope) {
                RecordOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    sheetState = sheetState,
                    scope = scope,
                )
            }.also {
                it.signedInProfileId = signedInProfileId
                it.recentConversations = recentConversations
            }

            RecordOptionsBottomSheet(
                state = state,
                onShareInConversationClicked = onShareInConversationClicked,
            )

            return state
        }
    }
}

@Composable
private fun RecordOptionsBottomSheet(
    state: RecordOptionsSheetState,
    onShareInConversationClicked: (RecordUri, Conversation) -> Unit,
) {
    val signedInProfileId = state.signedInProfileId
    if (state.showBottomSheet && signedInProfileId != null) {
        ModalBottomSheet(
            onDismissRequest = { state.showBottomSheet = false },
            sheetState = state.sheetState,
            content = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SendDirectMessageCard(
                        signedInProfileId = signedInProfileId,
                        recentConversations = state.recentConversations,
                        onConversationClicked = { conversation ->
                            state.scope.launch {
                                state.hideSheet()
                                state.currentRecordUri?.let { uri ->
                                    onShareInConversationClicked(uri, conversation)
                                }
                            }
                        },
                    )

                    state.currentRecordUri?.let { uri ->
                        CopyToClipboardCard(uri.shareUri())
                    }
                }
            },
        )
    }
}

fun RecordUri.shareUri(): GenericUri = GenericUri(
    when (this) {
        is FeedGeneratorUri -> "https://bsky.app/feed/$uri"
        is ListUri -> "https://bsky.app/list/$uri"
        is StarterPackUri -> "https://bsky.app/starter-pack/$uri"
        is LabelerUri -> "https://bsky.app/labeler/$uri"
        else -> uri
    },
)
