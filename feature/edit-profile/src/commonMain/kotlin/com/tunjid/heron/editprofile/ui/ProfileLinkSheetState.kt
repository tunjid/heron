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

package com.tunjid.heron.editprofile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileLinkValidation
import com.tunjid.heron.data.core.models.ProfileLinks
import com.tunjid.heron.data.core.models.validateProfileLinkInput
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.editprofile.EditableLink
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Link
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.Validator
import com.tunjid.heron.ui.text.message
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.link_add
import heron.feature.edit_profile.generated.resources.link_add_title
import heron.feature.edit_profile.generated.resources.link_edit_title
import heron.feature.edit_profile.generated.resources.link_error_duplicate
import heron.feature.edit_profile.generated.resources.link_error_insecure
import heron.feature.edit_profile.generated.resources.link_error_invalid
import heron.feature.edit_profile.generated.resources.link_error_shortener
import heron.feature.edit_profile.generated.resources.link_error_title_length
import heron.feature.edit_profile.generated.resources.link_remove
import heron.feature.edit_profile.generated.resources.link_save
import heron.feature.edit_profile.generated.resources.link_sheet_description
import heron.feature.edit_profile.generated.resources.link_title
import heron.feature.edit_profile.generated.resources.link_title_count
import heron.feature.edit_profile.generated.resources.link_title_placeholder
import heron.feature.edit_profile.generated.resources.link_url
import heron.feature.edit_profile.generated.resources.link_url_placeholder
import org.jetbrains.compose.resources.stringResource

@Stable
class ProfileLinkSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    internal var request by mutableStateOf<Request?>(null)

    override fun onHidden() {
        request = null
    }

    fun addLink(
        existingUrls: List<GenericUri>,
    ) {
        request = Request(
            link = null,
            existingUrls = existingUrls,
        )
        show()
    }

    fun editLink(
        link: EditableLink,
        otherUrls: List<GenericUri>,
    ) {
        request = Request(
            link = link,
            existingUrls = otherUrls,
        )
        show()
    }

    internal data class Request(
        val link: EditableLink?,
        val existingUrls: List<GenericUri>,
    )

    companion object {
        @Composable
        fun rememberProfileLinkSheetState(
            onLinkSaved: (original: EditableLink?, saved: EditableLink) -> Unit,
            onLinkRemoved: (EditableLink) -> Unit,
        ): ProfileLinkSheetState {
            val state = rememberBottomSheetState {
                ProfileLinkSheetState(
                    scope = it,
                )
            }

            ProfileLinkBottomSheet(
                state = state,
                onLinkSaved = onLinkSaved,
                onLinkRemoved = onLinkRemoved,
            )

            return state
        }
    }
}

@Composable
private fun ProfileLinkBottomSheet(
    state: ProfileLinkSheetState,
    onLinkSaved: (original: EditableLink?, saved: EditableLink) -> Unit,
    onLinkRemoved: (EditableLink) -> Unit,
) {
    val request = state.request ?: return
    state.ModalBottomSheet {
        val link = request.link
        val focusManager = LocalFocusManager.current
        var urlInput by remember(request) {
            mutableStateOf(link?.url?.uri ?: "")
        }
        var titleInput by remember(request) {
            mutableStateOf(link?.title ?: "")
        }
        val urlValidator = remember(request) {
            linkUrlValidator(existingUrls = request.existingUrls)
        }
        // Errors show as soon as there's input to check, so an empty field isn't flagged
        val urlError = urlInput.takeIf(String::isNotEmpty)?.let(urlValidator::errorFor)
        val titleError = LinkTitleValidator.errorFor(titleInput)

        val save = save@{
            if (urlInput.isBlank() || urlError != null || titleError != null) return@save

            val url =
                // An untouched link keeps its saved URL
                if (link != null && urlInput.trim() == link.url.uri) link.url
                else validateProfileLinkInput(
                    input = urlInput,
                    existingUrls = request.existingUrls,
                ).let { it as? ProfileLinkValidation.Valid }
                    ?.url
                    ?: return@save

            onLinkSaved(
                link,
                EditableLink(
                    uri = link?.uri,
                    url = url,
                    title = titleInput.trim().ifEmpty { null },
                    // An icon is fetched when the link is saved, so a new
                    // destination doesn't keep the old one's icon
                    icon = link?.icon?.takeIf { url == link.url },
                ),
            )
            state.hide()
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (link == null) Res.string.link_add_title
                    else Res.string.link_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(Res.string.link_sheet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(stringResource(Res.string.link_url)) },
                placeholder = { Text(stringResource(Res.string.link_url_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = HeronIcons.Regular.Link,
                        contentDescription = null,
                    )
                },
                isError = urlError != null,
                supportingText = urlError?.let {
                    { Text(it.message) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text(stringResource(Res.string.link_title)) },
                placeholder = { Text(stringResource(Res.string.link_title_placeholder)) },
                isError = titleError != null,
                supportingText = {
                    Text(
                        text = titleError?.message ?: stringResource(
                            Res.string.link_title_count,
                            titleInput.trim().length,
                            ProfileLinks.MAX_TITLE_LENGTH,
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { save() },
                ),
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = urlInput.isNotBlank() && urlError == null && titleError == null,
                onClick = save,
            ) {
                Text(
                    text = stringResource(
                        if (link == null) Res.string.link_add
                        else Res.string.link_save,
                    ),
                )
            }
            if (link != null) TextButton(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                onClick = {
                    onLinkRemoved(link)
                    state.hide()
                },
            ) {
                Text(text = stringResource(Res.string.link_remove))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * validateProfileLinkInput is the source of truth; each check passes unless the
 * input fails with that check's error.
 */
private fun linkUrlValidator(
    existingUrls: List<GenericUri>,
) = Validator(
    checks = LinkValidationErrors.map { (error, message) ->
        val check: (String) -> Boolean = { input ->
            validateProfileLinkInput(
                input = input,
                existingUrls = existingUrls,
            ) != error
        }
        check to Memo.Resource(message)
    },
)

private val LinkTitleValidator = Validator(
    Pair(
        { title: String ->
            title.trim().length <= ProfileLinks.MAX_TITLE_LENGTH
        },
        Memo.Resource(
            Res.string.link_error_title_length,
            listOf(ProfileLinks.MAX_TITLE_LENGTH),
        ),
    ),
)

private fun Validator.errorFor(
    text: String,
): Memo? = checks.firstNotNullOfOrNull { (check, message) ->
    if (check(text)) null
    else message
}

private val LinkValidationErrors = listOf(
    ProfileLinkValidation.Invalid to Res.string.link_error_invalid,
    ProfileLinkValidation.Insecure to Res.string.link_error_insecure,
    ProfileLinkValidation.Shortener to Res.string.link_error_shortener,
    ProfileLinkValidation.Duplicate to Res.string.link_error_duplicate,
)
