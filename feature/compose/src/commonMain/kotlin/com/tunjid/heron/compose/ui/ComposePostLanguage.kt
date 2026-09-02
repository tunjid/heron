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

package com.tunjid.heron.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.ml.language.isoLanguageTags
import com.tunjid.heron.data.ml.language.languageDisplayName
import com.tunjid.heron.data.ml.language.localeCollator
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.post_language
import heron.feature.compose.generated.resources.post_language_search
import heron.feature.compose.generated.resources.post_language_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ComposePostLanguage(
    modifier: Modifier = Modifier,
    languages: List<String>,
    onLanguagesChanged: (List<String>) -> Unit,
) {
    val sheetState = PostLanguageSheetState.rememberPostLanguageSheetState(
        selected = languages,
        onLanguagesChanged = onLanguagesChanged,
    )

    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
        onClick = sheetState::show,
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = stringResource(Res.string.post_language),
                tint = MaterialTheme.colorScheme.outline,
            )
            val label = languages.chipLabel()
            if (label.isNotEmpty()) Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Stable
class PostLanguageSheetState internal constructor(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    override fun onHidden() = Unit

    companion object {
        @Composable
        fun rememberPostLanguageSheetState(
            selected: List<String>,
            onLanguagesChanged: (List<String>) -> Unit,
        ): PostLanguageSheetState {
            val state = rememberBottomSheetState(
                block = ::PostLanguageSheetState,
            )
            PostLanguageBottomSheet(
                state = state,
                selected = selected,
                onLanguagesChanged = onLanguagesChanged,
            )
            return state
        }
    }
}

@Composable
private fun PostLanguageBottomSheet(
    state: PostLanguageSheetState,
    selected: List<String>,
    onLanguagesChanged: (List<String>) -> Unit,
) {
    state.ModalBottomSheet {
        val options = rememberLanguageOptions()
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, options) {
            if (query.isBlank()) options
            else options.filter { option ->
                option.displayName.contains(query, ignoreCase = true) ||
                    option.tag.contains(query, ignoreCase = true)
            }
        }

        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp),
            text = stringResource(Res.string.post_language_title),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(text = stringResource(Res.string.post_language_search)) },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .navigationBarsPadding(),
        ) {
            items(
                items = filtered,
                key = LanguageOption::tag,
            ) { option ->
                val isSelected = option.tag in selected
                val atLimit = !isSelected && selected.size >= MaxPostLanguages
                ListItem(
                    modifier = Modifier
                        .clickable(enabled = !atLimit) {
                            onLanguagesChanged(selected.toggled(option.tag))
                        }
                        .alpha(if (atLimit) 0.4f else 1f),
                    headlineContent = {
                        Text(text = option.displayName)
                    },
                    supportingContent = {
                        Text(text = option.tag)
                    },
                    trailingContent = {
                        if (isSelected) Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

private data class LanguageOption(
    val tag: String,
    val displayName: String,
)

@Composable
private fun rememberLanguageOptions(): List<LanguageOption> {
    // Render each language's name in the reader's current UI locale (e.g. "allemand" for a French
    // reader), recomputing if that locale changes.
    val localeTag = Locale.current.language
    return remember(localeTag) {
        isoLanguageTags()
            .distinct()
            .map { tag ->
                LanguageOption(
                    tag = tag,
                    displayName = languageDisplayName(
                        languageTag = tag,
                        inLocaleTag = localeTag,
                    ),
                )
            }
            .sortedWith(compareBy(localeCollator(localeTag), LanguageOption::displayName))
    }
}

private fun List<String>.chipLabel(): String =
    when (size) {
        0 -> ""
        1 -> first().uppercase()
        else -> "${first().uppercase()} +${size - 1}"
    }

private fun List<String>.toggled(
    tag: String,
): List<String> =
    if (tag in this) this - tag
    else (this + tag).takeLast(MaxPostLanguages)

private const val MaxPostLanguages = 3
