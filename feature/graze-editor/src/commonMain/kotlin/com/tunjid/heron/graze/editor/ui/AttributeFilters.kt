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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.attribute_compare
import heron.feature.graze_editor.generated.resources.embed_type
import heron.feature.graze_editor.generated.resources.selector
import heron.feature.graze_editor.generated.resources.value
import org.jetbrains.compose.resources.stringResource

@Composable
fun AttributeCompareFilter(
    filter: Filter.Attribute.Compare,
    onUpdate: (Filter.Attribute.Compare) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(
        onRemove = onRemove,
    ) {
        Text(
            text = stringResource(Res.string.attribute_compare),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp),
        )
        OutlinedTextField(
            value = filter.selector,
            onValueChange = { onUpdate(filter.copy(selector = it)) },
            label = { Text(text = stringResource(Res.string.selector)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries +
                    Filter.Comparator.Range.entries +
                    Filter.Comparator.Set.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(8.dp),
            )
            OutlinedTextField(
                value = filter.targetValue,
                onValueChange = { onUpdate(filter.copy(targetValue = it)) },
                label = { Text(text = stringResource(Res.string.value)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun AttributeEmbedFilter(
    filter: Filter.Attribute.Embed,
    onUpdate: (Filter.Attribute.Embed) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(
        onRemove = onRemove,
    ) {
        Text(
            text = stringResource(Res.string.embed_type),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier
                    .weight(1f),
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp),
            )
            // Enum dropdown for Embed Kind
            EmbedKindDropdown(
                selected = filter.embedType,
                onSelect = { onUpdate(filter.copy(embedType = it)) },
                modifier = Modifier
                    .weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmbedKindDropdown(
    selected: Filter.Attribute.Embed.Kind,
    onSelect: (Filter.Attribute.Embed.Kind) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Filter.Attribute.Embed.Kind.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.name) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
