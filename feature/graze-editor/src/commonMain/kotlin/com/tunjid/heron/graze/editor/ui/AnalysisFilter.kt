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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import heron.feature.graze_editor.generated.resources.threshold_percent
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
    modifier: Modifier = Modifier,
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

    FilterCard(
        modifier = modifier,
        onRemove = onRemove,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp),
        )
        OutlinedTextField(
            value = filter.category,
            onValueChange = { newCategory ->
                onUpdate(
                    when (filter) {
                        is Filter.Analysis.Language -> filter.copy(category = newCategory)
                        is Filter.Analysis.Sentiment -> filter.copy(category = newCategory)
                        is Filter.Analysis.FinancialSentiment -> filter.copy(category = newCategory)
                        is Filter.Analysis.Emotion -> filter.copy(category = newCategory)
                        is Filter.Analysis.Toxicity -> filter.copy(category = newCategory)
                        is Filter.Analysis.Topic -> filter.copy(category = newCategory)
                        is Filter.Analysis.TextArbitrary -> filter.copy(category = newCategory)
                        is Filter.Analysis.ImageNsfw -> filter.copy(category = newCategory)
                        is Filter.Analysis.ImageArbitrary -> filter.copy(category = newCategory)
                    },
                )
            },
            label = { Text(text = categoryLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { newOperator ->
                    onUpdate(
                        when (filter) {
                            is Filter.Analysis.Language -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Sentiment -> filter.copy(operator = newOperator)
                            is Filter.Analysis.FinancialSentiment -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Emotion -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Toxicity -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Topic -> filter.copy(operator = newOperator)
                            is Filter.Analysis.TextArbitrary -> filter.copy(operator = newOperator)
                            is Filter.Analysis.ImageNsfw -> filter.copy(operator = newOperator)
                            is Filter.Analysis.ImageArbitrary -> filter.copy(operator = newOperator)
                        },
                    )
                },
                modifier = Modifier
                    .weight(1f),
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.threshold_percent, filter.thresholdPercent),
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = filter.threshold.toFloat(),
                    onValueChange = { threshold ->
                        onUpdate(
                            when (filter) {
                                is Filter.Analysis.Language -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.Sentiment -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.FinancialSentiment -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.Emotion -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.Toxicity -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.Topic -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.TextArbitrary -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.ImageNsfw -> filter.copy(threshold = threshold.toDouble())
                                is Filter.Analysis.ImageArbitrary -> filter.copy(threshold = threshold.toDouble())
                            },
                        )
                    },
                    valueRange = 0.1f..1f,
                )
            }
        }
    }
}
