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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.entity_excludes
import heron.feature.graze_editor.generated.resources.entity_matches
import heron.feature.graze_editor.generated.resources.entity_type
import heron.feature.graze_editor.generated.resources.values_comma_separated
import org.jetbrains.compose.resources.stringResource

@Composable
fun EntityMatchesFilter(
    filter: Filter.Entity.Matches,
    onUpdate: (Filter.Entity.Matches) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text(stringResource(Res.string.entity_matches), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.entityType,
            onValueChange = { onUpdate(filter.copy(entityType = it)) },
            label = { Text(stringResource(Res.string.entity_type)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.values.joinToString(", "),
            onValueChange = { onUpdate(filter.copy(values = it.split(",").map { s -> s.trim() })) },
            label = { Text(stringResource(Res.string.values_comma_separated)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EntityExcludesFilter(
    filter: Filter.Entity.Excludes,
    onUpdate: (Filter.Entity.Excludes) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text(stringResource(Res.string.entity_excludes), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.entityType,
            onValueChange = { onUpdate(filter.copy(entityType = it)) },
            label = { Text(stringResource(Res.string.entity_type)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.values.joinToString(", "),
            onValueChange = { onUpdate(filter.copy(values = it.split(",").map { s -> s.trim() })) },
            label = { Text(stringResource(Res.string.values_comma_separated)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
