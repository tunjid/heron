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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_item
import heron.feature.graze_editor.generated.resources.comparator_equal
import heron.feature.graze_editor.generated.resources.comparator_greater_than
import heron.feature.graze_editor.generated.resources.comparator_greater_than_or_equal
import heron.feature.graze_editor.generated.resources.comparator_in
import heron.feature.graze_editor.generated.resources.comparator_less_than
import heron.feature.graze_editor.generated.resources.comparator_less_than_or_equal
import heron.feature.graze_editor.generated.resources.comparator_not_equal
import heron.feature.graze_editor.generated.resources.comparator_not_in
import heron.feature.graze_editor.generated.resources.remove_filter
import heron.feature.graze_editor.generated.resources.threshold_percent
import heron.ui.core.generated.resources.cancel
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterCard(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    tint: Color = Color.Unspecified,
    onRemove: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = tint,
        ),
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    title()
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.remove_filter),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun StandardFilter(
    modifier: Modifier = Modifier,
    title: String,
    tint: Color = Color.Unspecified,
    onRemove: () -> Unit,
    startContent: @Composable () -> Unit,
    endContent: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
) {
    FilterCard(
        modifier = modifier,
        tint = tint,
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                startContent()
            }
            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                endContent()
            }
        }
        additionalContent()
    }
}

@Composable
fun ChipFilter(
    modifier: Modifier = Modifier,
    title: String,
    tint: Color = Color.Unspecified,
    items: List<String>,
    onItemsUpdated: (List<String>) -> Unit,
    onRemove: () -> Unit,
    startContent: @Composable () -> Unit,
    endContent: @Composable () -> Unit,
) {
    StandardFilter(
        modifier = modifier,
        tint = tint,
        title = title,
        onRemove = onRemove,
        startContent = startContent,
        endContent = endContent,
        additionalContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                content = {
                    val addTextSheetState = rememberAddTextSheetState(
                        title = title,
                        onTextConfirmed = {
                            onItemsUpdated(items.plus(it).distinct())
                        },
                    )
                    items.forEach {
                        InputChip(
                            selected = false,
                            onClick = {
                            },
                            trailingIcon = {
                                Icon(
                                    modifier = Modifier
                                        .clickable {
                                            onItemsUpdated(items.minus(it))
                                        },
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = stringResource(CommonStrings.cancel),
                                )
                            },
                            label = {
                                Text(text = it)
                            },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = {
                            addTextSheetState.show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(text = stringResource(Res.string.add_item))
                    }
                },
            )
        },
    )
}

@Composable
fun ThresholdSlider(
    threshold: Double,
    onThresholdChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thresholdPercent = (threshold * 100).roundToInt()
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.threshold_percent, thresholdPercent),
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = threshold.toFloat(),
            onValueChange = { onThresholdChanged(it.toDouble()) },
            valueRange = 0.1f..1f,
        )
    }
}

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
