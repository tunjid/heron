/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.inference.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.inference.ModelItem
import heron.feature.inference.generated.resources.Res
import heron.feature.inference.generated.resources.ability_summary
import heron.feature.inference.generated.resources.ability_translation
import heron.feature.inference.generated.resources.cancel
import heron.feature.inference.generated.resources.download
import heron.feature.inference.generated.resources.download_failed
import heron.feature.inference.generated.resources.downloading
import heron.feature.inference.generated.resources.queued
import heron.feature.inference.generated.resources.retry
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModelCard(
    modifier: Modifier = Modifier,
    item: ModelItem,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.model.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatModelSize(item.model.sizeInBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.model.abilities.isNotEmpty()) FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.model.abilities.forEach { ability ->
                    AbilityChip(ability = ability)
                }
            }
            ModelStatus(
                status = item.status,
                onDownload = onDownload,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun ModelStatus(
    status: TaskStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    when (status) {
        is TaskStatus.Running -> {
            val fraction = status.progress?.fraction
            if (fraction != null) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { fraction },
            ) else LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            StatusRow(
                label = when (fraction) {
                    null -> stringResource(Res.string.downloading)
                    else -> "${stringResource(Res.string.downloading)} ${(fraction * 100).roundToInt()}%"
                },
                action = {
                    OutlinedButton(onClick = onCancel) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                },
            )
        }
        TaskStatus.Created -> StatusRow(
            label = stringResource(Res.string.queued),
            action = {
                OutlinedButton(onClick = onCancel) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
        is TaskStatus.Failed -> StatusRow(
            label = stringResource(Res.string.download_failed),
            labelColor = MaterialTheme.colorScheme.error,
            action = {
                Button(onClick = onDownload) {
                    Text(text = stringResource(Res.string.retry))
                }
            },
        )
        TaskStatus.NotFound -> StatusRow(
            label = null,
            action = {
                Button(onClick = onDownload) {
                    Text(text = stringResource(Res.string.download))
                }
            },
        )
    }
}

@Composable
private fun StatusRow(
    label: String?,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        action()
    }
}

@Composable
private fun AbilityChip(
    ability: InferenceModel.Ability,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
            text = ability.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun InferenceModel.Ability.label(): String = when (this) {
    InferenceModel.Ability.Translation -> stringResource(Res.string.ability_translation)
    InferenceModel.Ability.Summary -> stringResource(Res.string.ability_summary)
}

private fun formatModelSize(
    bytes: Long,
): String {
    val gigabyte = 1024L * 1024L * 1024L
    val megabyte = 1024L * 1024L
    return when {
        bytes >= gigabyte -> {
            val gigabytes = (bytes.toDouble() / gigabyte * 10).roundToInt() / 10.0
            "$gigabytes GB"
        }
        else -> "${bytes / megabyte} MB"
    }
}
