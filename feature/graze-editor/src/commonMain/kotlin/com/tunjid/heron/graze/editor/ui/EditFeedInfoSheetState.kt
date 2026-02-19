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

package com.tunjid.heron.graze.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.done
import heron.feature.graze_editor.generated.resources.edit_feed_description
import heron.feature.graze_editor.generated.resources.edit_feed_name
import org.jetbrains.compose.resources.stringResource

@Stable
class EditFeedInfoSheetState(scope: BottomSheetScope) : BottomSheetState(scope) {
    var name by mutableStateOf("")
    var description by mutableStateOf("")

    override fun onHidden() {
        name = ""
        description = ""
    }

    fun show(currentName: String, currentDescription: String?) {
        name = currentName
        description = currentDescription ?: ""
        show()
    }
}

@Composable
fun rememberEditFeedInfoSheetState(
    onInfoConfirmed: (String, String?) -> Unit
): EditFeedInfoSheetState {
    val state = rememberBottomSheetState { scope -> EditFeedInfoSheetState(scope = scope) }
    EditFeedInfoBottomSheet(state = state, onInfoConfirmed = onInfoConfirmed)
    return state
}

@Composable
private fun EditFeedInfoBottomSheet(
    state: EditFeedInfoSheetState,
    onInfoConfirmed: (String, String?) -> Unit,
) {
    state.ModalBottomSheet {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.edit_feed_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { state.name = it },
                label = { Text(stringResource(Res.string.edit_feed_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { state.description = it },
                label = { Text(stringResource(Res.string.edit_feed_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
            Button(
                onClick = {
                    val name = state.name
                    val description = state.description
                    if (name.isNotBlank()) {
                        onInfoConfirmed(name, description.takeIf { it.isNotBlank() })
                        state.hide()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank(),
            ) {
                Text(text = stringResource(Res.string.done))
            }
        }
    }
}
