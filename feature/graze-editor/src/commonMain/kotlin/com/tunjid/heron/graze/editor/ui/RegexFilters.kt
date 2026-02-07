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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.case_insensitive
import heron.feature.graze_editor.generated.resources.pattern
import heron.feature.graze_editor.generated.resources.regex_any
import heron.feature.graze_editor.generated.resources.regex_matches
import heron.feature.graze_editor.generated.resources.regex_negation
import heron.feature.graze_editor.generated.resources.regex_none
import heron.feature.graze_editor.generated.resources.terms_comma_separated
import heron.feature.graze_editor.generated.resources.variable
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegexMatchesFilter(
    filter: Filter.Regex.Matches,
    onUpdate: (Filter.Regex.Matches) -> Unit,
    onRemove: () -> Unit,
) {
    RegexBaseFilter(
        title = stringResource(Res.string.regex_matches),
        variable = filter.variable,
        pattern = filter.pattern,
        isCaseInsensitive = filter.isCaseInsensitive,
        onUpdateVariable = { onUpdate(filter.copy(variable = it)) },
        onUpdatePattern = { onUpdate(filter.copy(pattern = it)) },
        onUpdateCaseInsensitive = { onUpdate(filter.copy(isCaseInsensitive = it)) },
        onRemove = onRemove,
    )
}

@Composable
fun RegexNegationFilter(
    filter: Filter.Regex.Negation,
    onUpdate: (Filter.Regex.Negation) -> Unit,
    onRemove: () -> Unit,
) {
    RegexBaseFilter(
        title = stringResource(Res.string.regex_negation),
        variable = filter.variable,
        pattern = filter.pattern,
        isCaseInsensitive = filter.isCaseInsensitive,
        onUpdateVariable = { onUpdate(filter.copy(variable = it)) },
        onUpdatePattern = { onUpdate(filter.copy(pattern = it)) },
        onUpdateCaseInsensitive = { onUpdate(filter.copy(isCaseInsensitive = it)) },
        onRemove = onRemove,
    )
}

@Composable
private fun RegexBaseFilter(
    title: String,
    variable: String,
    pattern: String,
    isCaseInsensitive: Boolean,
    onUpdateVariable: (String) -> Unit,
    onUpdatePattern: (String) -> Unit,
    onUpdateCaseInsensitive: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(
        onRemove = onRemove,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = variable,
            onValueChange = onUpdateVariable,
            label = { Text(text = stringResource(Res.string.variable)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = pattern,
            onValueChange = onUpdatePattern,
            label = { Text(text = stringResource(Res.string.pattern)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isCaseInsensitive,
                onCheckedChange = onUpdateCaseInsensitive,
            )
            Spacer(
                modifier = Modifier.width(8.dp),
            )
            Text(text = stringResource(Res.string.case_insensitive))
        }
    }
}

@Composable
fun RegexAnyFilter(
    filter: Filter.Regex.Any,
    onUpdate: (Filter.Regex.Any) -> Unit,
    onRemove: () -> Unit,
) {
    RegexListBaseFilter(
        title = stringResource(Res.string.regex_any),
        variable = filter.variable,
        terms = filter.terms,
        isCaseInsensitive = filter.isCaseInsensitive,
        onUpdateVariable = { onUpdate(filter.copy(variable = it)) },
        onUpdateTerms = { onUpdate(filter.copy(terms = it)) },
        onUpdateCaseInsensitive = { onUpdate(filter.copy(isCaseInsensitive = it)) },
        onRemove = onRemove,
    )
}

@Composable
fun RegexNoneFilter(
    filter: Filter.Regex.None,
    onUpdate: (Filter.Regex.None) -> Unit,
    onRemove: () -> Unit,
) {
    RegexListBaseFilter(
        title = stringResource(Res.string.regex_none),
        variable = filter.variable,
        terms = filter.terms,
        isCaseInsensitive = filter.isCaseInsensitive,
        onUpdateVariable = { onUpdate(filter.copy(variable = it)) },
        onUpdateTerms = { onUpdate(filter.copy(terms = it)) },
        onUpdateCaseInsensitive = { onUpdate(filter.copy(isCaseInsensitive = it)) },
        onRemove = onRemove,
    )
}

@Composable
private fun RegexListBaseFilter(
    title: String,
    variable: String,
    terms: List<String>,
    isCaseInsensitive: Boolean,
    onUpdateVariable: (String) -> Unit,
    onUpdateTerms: (List<String>) -> Unit,
    onUpdateCaseInsensitive: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(
        onRemove = onRemove,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = variable,
            onValueChange = onUpdateVariable,
            label = { Text(text = stringResource(Res.string.variable)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = terms.joinToString(", "),
            onValueChange = { onUpdateTerms(it.split(",").map { s -> s.trim() }) },
            label = { Text(text = stringResource(Res.string.terms_comma_separated)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isCaseInsensitive,
                onCheckedChange = onUpdateCaseInsensitive,
            )
            Spacer(
                modifier = Modifier.width(8.dp),
            )
            Text(text = stringResource(Res.string.case_insensitive))
        }
    }
}
