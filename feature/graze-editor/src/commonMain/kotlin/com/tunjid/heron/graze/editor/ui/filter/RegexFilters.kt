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

package com.tunjid.heron.graze.editor.ui.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.sheets.SelectTextSheetState.Companion.rememberSelectTextState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_item
import heron.feature.graze_editor.generated.resources.case_insensitive
import heron.feature.graze_editor.generated.resources.pattern
import heron.feature.graze_editor.generated.resources.regex_any
import heron.feature.graze_editor.generated.resources.regex_matches
import heron.feature.graze_editor.generated.resources.regex_negation
import heron.feature.graze_editor.generated.resources.regex_none
import heron.feature.graze_editor.generated.resources.variable
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegexFilter(
    filter: Filter.Regex,
    onUpdate: (Filter.Regex) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        tint = filter.validationTint(),
        title = stringResource(
            when (filter) {
                is Filter.Regex.Matches -> Res.string.regex_matches
                is Filter.Regex.Negation -> Res.string.regex_negation
                is Filter.Regex.Any -> Res.string.regex_any
                is Filter.Regex.None -> Res.string.regex_none
            },
        ),
        onRemove = onRemove,
        rowContent = {
            Dropdown(
                label = stringResource(Res.string.variable),
                selected = filter.selectedVariable,
                options = Filter.Attribute.Compare.Selector.entries,
                stringRes = Filter.Attribute.Compare.Selector::stringRes,
                onSelect = { onUpdate(filter.withVariable(it.value)) },
                modifier = Modifier
                    .fillMaxWidth(),
            )
        },
        additionalContent = {
            when (filter) {
                is Filter.Regex.Matches -> LabeledTextEntry(
                    label = stringResource(Res.string.pattern),
                    value = filter.pattern,
                    onValueChanged = { onUpdate(filter.copy(pattern = it)) },
                )

                is Filter.Regex.Negation -> LabeledTextEntry(
                    label = stringResource(Res.string.pattern),
                    value = filter.pattern,
                    onValueChanged = { onUpdate(filter.copy(pattern = it)) },
                )

                is Filter.Regex.Any -> RegexTerms(
                    terms = filter.terms,
                    onTermsChanged = { onUpdate(filter.copy(terms = it)) },
                )

                is Filter.Regex.None -> RegexTerms(
                    terms = filter.terms,
                    onTermsChanged = { onUpdate(filter.copy(terms = it)) },
                )
            }
            CaseInsensitiveToggle(
                checked = filter.caseInsensitive,
                onCheckedChange = { onUpdate(filter.withCaseInsensitive(it)) },
            )
        },
    )
}

@Composable
private fun RegexTerms(
    terms: List<String>,
    onTermsChanged: (List<String>) -> Unit,
) {
    val selectTextSheetState = rememberSelectTextState(
        title = stringResource(Res.string.add_item),
        items = terms,
        onItemsUpdated = onTermsChanged,
    )
    FilterTextChips(
        selectTextSheetState = selectTextSheetState,
        buttonStringResource = Res.string.add_item,
        onItemsUpdated = onTermsChanged,
        items = terms,
    )
}

@Composable
private fun CaseInsensitiveToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.case_insensitive),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private val Filter.Regex.currentVariable: String
    get() = when (this) {
        is Filter.Regex.Matches -> variable
        is Filter.Regex.Negation -> variable
        is Filter.Regex.Any -> variable
        is Filter.Regex.None -> variable
    }

private val Filter.Regex.caseInsensitive: Boolean
    get() = when (this) {
        is Filter.Regex.Matches -> isCaseInsensitive
        is Filter.Regex.Negation -> isCaseInsensitive
        is Filter.Regex.Any -> isCaseInsensitive
        is Filter.Regex.None -> isCaseInsensitive
    }

private val Filter.Regex.selectedVariable: Filter.Attribute.Compare.Selector
    get() = Filter.Attribute.Compare.Selector.entries
        .firstOrNull { it.value == currentVariable }
        ?: Filter.Attribute.Compare.Selector.Text

private fun Filter.Regex.withVariable(
    value: String,
): Filter.Regex = when (this) {
    is Filter.Regex.Matches -> copy(variable = value)
    is Filter.Regex.Negation -> copy(variable = value)
    is Filter.Regex.Any -> copy(variable = value)
    is Filter.Regex.None -> copy(variable = value)
}

private fun Filter.Regex.withCaseInsensitive(
    value: Boolean,
): Filter.Regex = when (this) {
    is Filter.Regex.Matches -> copy(isCaseInsensitive = value)
    is Filter.Regex.Negation -> copy(isCaseInsensitive = value)
    is Filter.Regex.Any -> copy(isCaseInsensitive = value)
    is Filter.Regex.None -> copy(isCaseInsensitive = value)
}
