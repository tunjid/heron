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
fun AnalysisFilter(
    filter: Filter.Analysis,
    onUpdate: (Filter.Analysis) -> Unit,
    onRemove: () -> Unit,
) {
    val title = when (filter) {
        is Filter.Analysis.Language -> "Language Analysis"
        is Filter.Analysis.Sentiment -> "Sentiment Analysis"
        is Filter.Analysis.FinancialSentiment -> "Financial Sentiment"
        is Filter.Analysis.Emotion -> "Emotion Analysis"
        is Filter.Analysis.Toxicity -> "Toxicity Analysis"
        is Filter.Analysis.Topic -> "Topic Analysis"
        is Filter.Analysis.TextArbitrary -> "Text Arbitrary"
        is Filter.Analysis.ImageNsfw -> "Image NSFW"
        is Filter.Analysis.ImageArbitrary -> "Image Arbitrary"
    }

    val categoryLabel = when (filter) {
        is Filter.Analysis.Language -> "Language Name"
        is Filter.Analysis.Sentiment -> "Sentiment Category"
        is Filter.Analysis.FinancialSentiment -> "Financial Category"
        is Filter.Analysis.Emotion -> "Emotion Category"
        is Filter.Analysis.Toxicity -> "Toxic Category"
        is Filter.Analysis.Topic -> "Topic Label"
        is Filter.Analysis.TextArbitrary -> "Tag"
        is Filter.Analysis.ImageNsfw -> "Tag"
        is Filter.Analysis.ImageArbitrary -> "Tag"
    }

    FilterCard(onRemove = onRemove) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.category,
            onValueChange = { newCategory ->
                 val updated = when (filter) {
                    is Filter.Analysis.Language -> filter.copy(category = newCategory)
                    is Filter.Analysis.Sentiment -> filter.copy(category = newCategory)
                    is Filter.Analysis.FinancialSentiment -> filter.copy(category = newCategory)
                    is Filter.Analysis.Emotion -> filter.copy(category = newCategory)
                    is Filter.Analysis.Toxicity -> filter.copy(category = newCategory)
                    is Filter.Analysis.Topic -> filter.copy(category = newCategory)
                    is Filter.Analysis.TextArbitrary -> filter.copy(category = newCategory)
                    is Filter.Analysis.ImageNsfw -> filter.copy(category = newCategory)
                    is Filter.Analysis.ImageArbitrary -> filter.copy(category = newCategory)
                }
                onUpdate(updated)
            },
            label = { Text(categoryLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { newOperator ->
                     val updated = when (filter) {
                        is Filter.Analysis.Language -> filter.copy(operator = newOperator)
                        is Filter.Analysis.Sentiment -> filter.copy(operator = newOperator)
                        is Filter.Analysis.FinancialSentiment -> filter.copy(operator = newOperator)
                        is Filter.Analysis.Emotion -> filter.copy(operator = newOperator)
                        is Filter.Analysis.Toxicity -> filter.copy(operator = newOperator)
                        is Filter.Analysis.Topic -> filter.copy(operator = newOperator)
                        is Filter.Analysis.TextArbitrary -> filter.copy(operator = newOperator)
                        is Filter.Analysis.ImageNsfw -> filter.copy(operator = newOperator)
                        is Filter.Analysis.ImageArbitrary -> filter.copy(operator = newOperator)
                    }
                    onUpdate(updated)
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.threshold.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { d ->
                         val updated = when (filter) {
                            is Filter.Analysis.Language -> filter.copy(threshold = d)
                            is Filter.Analysis.Sentiment -> filter.copy(threshold = d)
                            is Filter.Analysis.FinancialSentiment -> filter.copy(threshold = d)
                            is Filter.Analysis.Emotion -> filter.copy(threshold = d)
                            is Filter.Analysis.Toxicity -> filter.copy(threshold = d)
                            is Filter.Analysis.Topic -> filter.copy(threshold = d)
                            is Filter.Analysis.TextArbitrary -> filter.copy(threshold = d)
                            is Filter.Analysis.ImageNsfw -> filter.copy(threshold = d)
                            is Filter.Analysis.ImageArbitrary -> filter.copy(threshold = d)
                        }
                        onUpdate(updated)
                    }
                },
                label = { Text("Threshold") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
