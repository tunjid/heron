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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.comparator_equal
import heron.feature.graze_editor.generated.resources.comparator_greater_than
import heron.feature.graze_editor.generated.resources.comparator_greater_than_or_equal
import heron.feature.graze_editor.generated.resources.comparator_in
import heron.feature.graze_editor.generated.resources.comparator_less_than
import heron.feature.graze_editor.generated.resources.comparator_less_than_or_equal
import heron.feature.graze_editor.generated.resources.comparator_not_equal
import heron.feature.graze_editor.generated.resources.comparator_not_in
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource

@Composable
fun StandardFilter(
    title: String,
    onRemove: () -> Unit,
    comparison: @Composable () -> Unit,
    selection: @Composable () -> Unit,
    content: @Composable () -> Unit = {},
) {
    FilterCard(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
        },
        onRemove = onRemove,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                comparison()
            }
            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                selection()
            }
        }
        content()
    }
}

val Filter.ML.Similarity.thresholdPercent: Int
    get() = (threshold * 100).roundToInt()

val Filter.ML.Probability.thresholdPercent: Int
    get() = (threshold * 100).roundToInt()

val Filter.ML.Moderation.thresholdPercent: Int
    get() = (threshold * 100).roundToInt()

val Filter.Analysis.thresholdPercent: Int
    get() = (threshold * 100).roundToInt()

val Filter.Comparator.stringRes: StringResource
    get() = when (this) {
        Filter.Comparator.Equality.Equal -> Res.string.comparator_equal
        Filter.Comparator.Equality.NotEqual -> Res.string.comparator_not_equal
        Filter.Comparator.Range.GreaterThan -> Res.string.comparator_greater_than
        Filter.Comparator.Range.LessThan -> Res.string.comparator_less_than
        Filter.Comparator.Range.GreaterThanOrEqual -> Res.string.comparator_greater_than_or_equal
        Filter.Comparator.Range.LessThanOrEqual -> Res.string.comparator_less_than_or_equal
        Filter.Comparator.Set.In -> Res.string.comparator_in
        Filter.Comparator.Set.NotIn -> Res.string.comparator_not_in
    }
