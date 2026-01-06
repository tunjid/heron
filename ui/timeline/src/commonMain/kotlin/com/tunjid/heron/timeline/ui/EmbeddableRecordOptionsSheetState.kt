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

package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.timeline.utilities.CopyContentsToClipboardCard
import com.tunjid.heron.timeline.utilities.CopyLinkToClipboardCard
import com.tunjid.heron.timeline.utilities.SendDirectMessageCard
import com.tunjid.heron.timeline.utilities.ShareInPostCard
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

@Stable
class EmbeddableRecordOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentRecordUri: EmbeddableRecordUri? by mutableStateOf(null)

    internal var currentRecordText: String? by mutableStateOf(null)

    internal val isSignedIn get() = signedInProfileId != null

    override fun onHidden() {
        currentRecordUri = null
        currentRecordText = null
    }

    fun showOptions(recordUri: EmbeddableRecordUri, recordText: String? = null) {
        currentRecordUri = recordUri
        currentRecordText = recordText
        show()
    }

    companion object Companion {
        @Composable
        fun rememberUpdatedEmbeddableRecordOptionsState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (EmbeddableRecordUri, Conversation) -> Unit,
            onShareInPostClicked: (EmbeddableRecordUri) -> Unit,
        ): EmbeddableRecordOptionsSheetState {
            val state = rememberBottomSheetState {
                EmbeddableRecordOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    scope = it,
                )
            }.also {
                it.signedInProfileId = signedInProfileId
                it.recentConversations = recentConversations
            }

            EmbeddableRecordOptionsBottomSheet(
                state = state,
                onShareInConversationClicked = onShareInConversationClicked,
                onShareInPostClicked = onShareInPostClicked,
            )

            return state
        }
    }
}

@Composable
private fun EmbeddableRecordOptionsBottomSheet(
    state: EmbeddableRecordOptionsSheetState,
    onShareInConversationClicked: (EmbeddableRecordUri, Conversation) -> Unit,
    onShareInPostClicked: (EmbeddableRecordUri) -> Unit,
) {
    val signedInProfileId = state.signedInProfileId

    if (signedInProfileId != null) state.ModalBottomSheet {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SendDirectMessageCard(
                signedInProfileId = signedInProfileId,
                recentConversations = state.recentConversations,
                onConversationClicked = { conversation ->
                    state.currentRecordUri?.let { uri ->
                        onShareInConversationClicked(uri, conversation)
                    }
                    state.hide()
                },
            )

            ShareInPostCard {
                state.currentRecordUri?.let { uri ->
                    onShareInPostClicked(uri)
                }
                state.hide()
            }

            state.currentRecordUri?.let { uri ->
                CopyLinkToClipboardCard(uri.shareUri())
            }

            state.currentRecordText?.let { text ->
                CopyContentsToClipboardCard(text)
            }
        }
    }
}
