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
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.timeline.utilities.CopyToClipboardCard
import com.tunjid.heron.timeline.utilities.SendDirectMessageCard
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

@Stable
class RecordOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentRecordUri: RecordUri? by mutableStateOf(null)

    internal val isSignedIn get() = signedInProfileId != null

    override fun onHidden() {
        currentRecordUri = null
    }

    fun showOptions(recordUri: RecordUri) {
        currentRecordUri = recordUri
        show()
    }

    companion object {
        @Composable
        fun rememberUpdatedRecordOptionsState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (RecordUri, Conversation) -> Unit,
        ): RecordOptionsSheetState {
            val state = rememberBottomSheetState {
                RecordOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    scope = it,
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

            state.currentRecordUri?.let { uri ->
                CopyToClipboardCard(uri.shareUri())
            }
        }
    }
}
