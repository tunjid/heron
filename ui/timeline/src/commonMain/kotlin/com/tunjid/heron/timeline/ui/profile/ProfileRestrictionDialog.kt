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

package com.tunjid.heron.timeline.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tunjid.heron.timeline.ui.post.PostOption
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.block_account_dialog_description
import heron.ui.core.generated.resources.block_account_dialog_title
import heron.ui.core.generated.resources.mute_account_dialog_description
import heron.ui.core.generated.resources.mute_account_dialog_title
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

@Stable
class ProfileRestrictionDialogState internal constructor() {

    var moderation by mutableStateOf<PostOption.Moderation.ProfileRestriction?>(null)
        private set

    fun show(moderation: PostOption.Moderation.ProfileRestriction) {
        this.moderation = moderation
    }

    fun hide() {
        moderation = null
    }

    companion object Companion {
        @Composable
        fun rememberProfileRestrictionDialogState(
            onProfileRestricted: (PostOption.Moderation.ProfileRestriction) -> Unit,
        ): ProfileRestrictionDialogState {
            val state = remember { ProfileRestrictionDialogState() }

            state.moderation?.let { moderation ->
                ProfileRestrictionDialog(
                    moderation = moderation,
                    onDismiss = state::hide,
                    onApproved = {
                        onProfileRestricted(moderation)
                        state.hide()
                    },
                )
            }

            return state
        }
    }
}

@Composable
private fun ProfileRestrictionDialog(
    moderation: PostOption.Moderation.ProfileRestriction,
    onDismiss: () -> Unit,
    onApproved: () -> Unit,
) {
    val (title, description, confirmText) = when (moderation) {
        is PostOption.Moderation.BlockAccount -> Triple(
            stringResource(CommonStrings.block_account_dialog_title),
            stringResource(CommonStrings.block_account_dialog_description),
            stringResource(CommonStrings.viewer_state_block_account),
        )
        is PostOption.Moderation.MuteAccount -> Triple(
            stringResource(CommonStrings.mute_account_dialog_title),
            stringResource(CommonStrings.mute_account_dialog_description),
            stringResource(CommonStrings.viewer_state_mute_account),
        )
    }

    SimpleDialog(
        onDismissRequest = onDismiss,
        title = { SimpleDialogTitle(text = title) },
        text = { SimpleDialogText(text = description) },
        confirmButton = {
            DestructiveDialogButton(
                text = confirmText,
                onClick = onApproved,
            )
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(Res.string.cancel),
                onClick = onDismiss,
            )
        },
    )
}
