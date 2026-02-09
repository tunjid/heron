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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import org.jetbrains.compose.resources.stringResource

@Stable
class AddTextSheetState(
    title: String,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    var title by mutableStateOf(title)
    var text by mutableStateOf("")
    var startingText by mutableStateOf("")

    override fun onHidden() {
        text = ""
        startingText = ""
    }

    fun show(currentText: String) {
        startingText = currentText
        text = currentText
        show()
    }
}

@Composable
fun rememberAddTextSheetState(
    title: String,
    onTextConfirmed: AddTextSheetState.(String) -> Unit,
): AddTextSheetState {
    val state = rememberBottomSheetState { scope ->
        AddTextSheetState(
            title = title,
            scope = scope,
        )
    }.also { it.title = title }
    AddTextBottomSheet(
        state = state,
        onTextConfirmed = onTextConfirmed,
    )
    return state
}

@Composable
private fun AddTextBottomSheet(
    state: AddTextSheetState,
    onTextConfirmed: AddTextSheetState.(String) -> Unit,
) {
    state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(vertical = 12.dp),
            )
            OutlinedTextField(
                value = state.text,
                onValueChange = { state.text = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (state.text.isNotBlank()) {
                        state.onTextConfirmed(state.text)
                    }
                    state.hide()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(text = stringResource(Res.string.done))
            }
        }
    }
}
