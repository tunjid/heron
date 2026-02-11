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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.profile.ProfileSearchResults
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.done
import org.jetbrains.compose.resources.stringResource

@Stable
class EditFilterTextSheetState(
    val options: Options,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    override fun onHidden() {
        options.text = ""
        options.startingText = ""
    }

    fun show(
        currentText: String,
    ) {
        options.startingText = currentText
        options.text = currentText
        show()
    }

    @Stable
    sealed class Options(
        title: String,
    ) {
        var title by mutableStateOf(title)
        var text by mutableStateOf("")
        var startingText by mutableStateOf("")

        sealed class Single(
            title: String,
        ) : Options(title) {

            class Text(
                title: String,
            ) : Options(title)

            class SuggestedProfiles(
                title: String,
            ) : Collection(title) {
                var profileSuggestions by mutableStateOf(emptyList<Profile>())
            }
        }

        sealed class Collection(
            title: String,
        ) : Options(title) {
            var items by mutableStateOf(emptyList<String>())

            class Text(
                title: String,
            ) : Collection(title)

            class SuggestedProfiles(
                title: String,
            ) : Collection(title) {
                var profileSuggestions by mutableStateOf(emptyList<Profile>())
            }
        }
    }

    companion object {
        @Composable
        fun rememberEditFilterTextState(
            title: String,
            onTextConfirmed: Options.(String) -> Unit,
        ): EditFilterTextSheetState {
            val options = remember {
                Options.Single.Text(title)
            }.also {
                it.title = title
            }
            return rememberEditFilterTextState(
                options = options,
                onTextConfirmed = onTextConfirmed,
            )
        }

        @Composable
        fun rememberEditFilterProfileTextState(
            title: String,
            suggestedProfiles: List<Profile>,
            onTextConfirmed: Options.(String) -> Unit,
        ): EditFilterTextSheetState {
            val options = remember {
                Options.Single.SuggestedProfiles(title)
            }.also {
                it.title = title
                it.profileSuggestions = suggestedProfiles
            }
            return rememberEditFilterTextState(
                options = options,
                onTextConfirmed = onTextConfirmed,
            )
        }

        @Composable
        fun rememberEditFilterTextState(
            title: String,
            items: List<String>,
            onItemsUpdated: (List<String>) -> Unit,
        ): EditFilterTextSheetState {
            val options = remember {
                Options.Collection.Text(title)
            }.also {
                it.title = title
                it.items = items
            }
            return rememberEditFilterTextState(
                options = options,
                onTextConfirmed = { updatedText ->
                    onItemsUpdated(
                        options.replaceOrAdd(items, updatedText),
                    )
                },
            )
        }

        @Composable
        fun rememberEditFilterProfileTextState(
            title: String,
            suggestedProfiles: List<Profile>,
            items: List<String>,
            onItemsUpdated: (List<String>) -> Unit,
        ): EditFilterTextSheetState {
            val options = remember {
                Options.Collection.SuggestedProfiles(title)
            }.also {
                it.title = title
                it.items = items
                it.profileSuggestions = suggestedProfiles
            }
            return rememberEditFilterTextState(
                options = options,
                onTextConfirmed = { updatedText ->
                    onItemsUpdated(
                        options.replaceOrAdd(items, updatedText),
                    )
                },
            )
        }

        @Composable
        private inline fun rememberEditFilterTextState(
            options: Options,
            crossinline onTextConfirmed: Options.(String) -> Unit,
        ): EditFilterTextSheetState {
            val state = rememberBottomSheetState { scope ->
                EditFilterTextSheetState(
                    options = options,
                    scope = scope,
                )
            }
            AddTextBottomSheet(
                state = state,
                onTextConfirmed = onTextConfirmed,
            )
            return state
        }

        @Composable
        private inline fun AddTextBottomSheet(
            state: EditFilterTextSheetState,
            crossinline onTextConfirmed: Options.(String) -> Unit,
        ) {
            state.ModalBottomSheet {
                val options = state.options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = options.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(vertical = 12.dp),
                    )
                    OutlinedTextField(
                        value = options.text,
                        onValueChange = { options.text = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )

                    if (options is Options.Collection.SuggestedProfiles) {
                        ProfileSearchResults(
                            modifier = Modifier
                                .fillMaxWidth(),
                            results = options.profileSuggestions,
                            onProfileClicked = { profile ->
                                options.onTextConfirmed(profile.handle.id)
                                state.hide()
                            },
                        )
                    }

                    Button(
                        onClick = {
                            if (options.text.isNotBlank()) {
                                options.onTextConfirmed(options.text)
                            }
                            state.hide()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.done))
                    }
                }
            }
        }
    }
}

private fun EditFilterTextSheetState.Options.Collection.replaceOrAdd(
    items: List<String>,
    updatedText: String,
): List<String> = items
    .minus(startingText)
    .plus(updatedText)
    .distinct()
