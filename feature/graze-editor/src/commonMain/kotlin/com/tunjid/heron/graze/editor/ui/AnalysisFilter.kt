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
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.emotion_analysis
import heron.feature.graze_editor.generated.resources.emotion_category
import heron.feature.graze_editor.generated.resources.financial_category
import heron.feature.graze_editor.generated.resources.financial_sentiment
import heron.feature.graze_editor.generated.resources.image_arbitrary
import heron.feature.graze_editor.generated.resources.image_nsfw
import heron.feature.graze_editor.generated.resources.language_analysis
import heron.feature.graze_editor.generated.resources.language_name
import heron.feature.graze_editor.generated.resources.sentiment_analysis
import heron.feature.graze_editor.generated.resources.sentiment_category
import heron.feature.graze_editor.generated.resources.tag
import heron.feature.graze_editor.generated.resources.text_arbitrary
import heron.feature.graze_editor.generated.resources.threshold
import heron.feature.graze_editor.generated.resources.topic_analysis
import heron.feature.graze_editor.generated.resources.topic_label
import heron.feature.graze_editor.generated.resources.toxic_category
import heron.feature.graze_editor.generated.resources.toxicity_analysis
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnalysisFilter(
    filter: Filter.Analysis,
    onUpdate: (Filter.Analysis) -> Unit,
    onRemove: () -> Unit,
) {
    val title = when (filter) {
        is Filter.Analysis.Language -> stringResource(Res.string.language_analysis)
        is Filter.Analysis.Sentiment -> stringResource(Res.string.sentiment_analysis)
        is Filter.Analysis.FinancialSentiment -> stringResource(Res.string.financial_sentiment)
        is Filter.Analysis.Emotion -> stringResource(Res.string.emotion_analysis)
        is Filter.Analysis.Toxicity -> stringResource(Res.string.toxicity_analysis)
        is Filter.Analysis.Topic -> stringResource(Res.string.topic_analysis)
        is Filter.Analysis.TextArbitrary -> stringResource(Res.string.text_arbitrary)
        is Filter.Analysis.ImageNsfw -> stringResource(Res.string.image_nsfw)
        is Filter.Analysis.ImageArbitrary -> stringResource(Res.string.image_arbitrary)
    }

    val categoryLabel = when (filter) {
        is Filter.Analysis.Language -> stringResource(Res.string.language_name)
        is Filter.Analysis.Sentiment -> stringResource(Res.string.sentiment_category)
        is Filter.Analysis.FinancialSentiment -> stringResource(Res.string.financial_category)
        is Filter.Analysis.Emotion -> stringResource(Res.string.emotion_category)
        is Filter.Analysis.Toxicity -> stringResource(Res.string.toxic_category)
        is Filter.Analysis.Topic -> stringResource(Res.string.topic_label)
        is Filter.Analysis.TextArbitrary -> stringResource(Res.string.tag)
        is Filter.Analysis.ImageNsfw -> stringResource(Res.string.tag)
        is Filter.Analysis.ImageArbitrary -> stringResource(Res.string.tag)
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
                label = { Text(stringResource(Res.string.threshold)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
