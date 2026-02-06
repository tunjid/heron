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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter

@Composable
fun MLSimilarityFilter(
    filter: Filter.ML.Similarity,
    onUpdate: (Filter.ML.Similarity) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Text Similarity", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.path,
            onValueChange = { onUpdate(filter.copy(path = it)) },
            label = { Text("Path") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.config.modelName,
            onValueChange = { onUpdate(filter.copy(config = filter.config.copy(modelName = it))) },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.config.anchorText,
            onValueChange = { onUpdate(filter.copy(config = filter.config.copy(anchorText = it))) },
            label = { Text("Anchor Text") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.threshold.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { d -> onUpdate(filter.copy(threshold = d)) }
                },
                label = { Text("Threshold") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun MLProbabilityFilter(
    filter: Filter.ML.Probability,
    onUpdate: (Filter.ML.Probability) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Model Probability", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.config.modelName,
            onValueChange = { onUpdate(filter.copy(config = filter.config.copy(modelName = it))) },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.threshold.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { d -> onUpdate(filter.copy(threshold = d)) }
                },
                label = { Text("Threshold") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun MLModerationFilter(
    filter: Filter.ML.Moderation,
    onUpdate: (Filter.ML.Moderation) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Content Moderation", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.category,
            onValueChange = { onUpdate(filter.copy(category = it)) },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries +
                        Filter.Comparator.Range.entries +
                        Filter.Comparator.Set.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.threshold.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { d -> onUpdate(filter.copy(threshold = d)) }
                },
                label = { Text("Threshold") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
