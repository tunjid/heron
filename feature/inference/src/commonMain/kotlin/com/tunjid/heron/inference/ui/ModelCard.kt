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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.ModeStandby
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.inference.ModelItem
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogState
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.rememberSimpleDialogState
import heron.feature.inference.generated.resources.Res
import heron.feature.inference.generated.resources.ability_summary
import heron.feature.inference.generated.resources.ability_translation
import heron.feature.inference.generated.resources.cancel
import heron.feature.inference.generated.resources.delete
import heron.feature.inference.generated.resources.delete_model_message
import heron.feature.inference.generated.resources.delete_model_title
import heron.feature.inference.generated.resources.download
import heron.feature.inference.generated.resources.load
import heron.feature.inference.generated.resources.load_error
import heron.feature.inference.generated.resources.loaded
import heron.feature.inference.generated.resources.loading
import heron.feature.inference.generated.resources.minimum_memory
import heron.feature.inference.generated.resources.retry
import heron.feature.inference.generated.resources.terms_of_use_link
import heron.feature.inference.generated.resources.terms_of_use_message
import heron.feature.inference.generated.resources.terms_of_use_title
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModelCard(
    modifier: Modifier = Modifier,
    engineState: EngineState?,
    item: ModelItem,
    downloadEnabled: Boolean = true,
    onLoad: (LoadedModel) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val termsDialogState = rememberSimpleDialogState()
    val deleteDialogState = rememberSimpleDialogState()
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.model.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (item.model.abilities.isNotEmpty()) FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.model.abilities.forEach { ability ->
                    AbilityChip(ability = ability)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetadataChip(
                    icon = Icons.Rounded.Storage,
                    text = formatModelSize(item.model.sizeInBytes),
                )
                MetadataChip(
                    icon = Icons.Rounded.Memory,
                    text = stringResource(
                        Res.string.minimum_memory,
                        item.model.minDeviceMemoryInGb,
                    ),
                )
                Spacer(Modifier.weight(1f))
                ModelActions(
                    engineState = engineState,
                    status = item.status,
                    downloadEnabled = downloadEnabled,
                    onLoad = onLoad,
                    onDownload = {
                        if (item.model.termsOfServiceUrl != null) termsDialogState.show()
                        else onDownload()
                    },
                    onCancel = onCancel,
                    onDeleteClick = deleteDialogState::show,
                )
            }
            TermsOfUseDialog(
                state = termsDialogState,
                model = item.model,
                onAccept = {
                    termsDialogState.hide()
                    onDownload()
                },
            )
            DeleteModelDialog(
                state = deleteDialogState,
                model = item.model,
                onConfirm = {
                    deleteDialogState.hide()
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun TermsOfUseDialog(
    state: SimpleDialogState,
    model: InferenceModel,
    onAccept: () -> Unit,
) {
    val termsUrl = model.termsOfServiceUrl ?: return
    val uriHandler = LocalUriHandler.current
    SimpleDialog(
        state = state,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.terms_of_use_title),
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SimpleDialogText(
                    text = stringResource(
                        Res.string.terms_of_use_message,
                        model.name,
                    ),
                )
                Text(
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri(termsUrl) }
                    },
                    text = stringResource(Res.string.terms_of_use_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            PrimaryDialogButton(
                text = stringResource(Res.string.download),
                onClick = onAccept,
            )
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(Res.string.cancel),
                onClick = state::hide,
            )
        },
    )
}

@Composable
private fun DeleteModelDialog(
    state: SimpleDialogState,
    model: InferenceModel,
    onConfirm: () -> Unit,
) {
    SimpleDialog(
        state = state,
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.delete_model_title),
            )
        },
        text = {
            SimpleDialogText(
                text = stringResource(
                    Res.string.delete_model_message,
                    model.name,
                ),
            )
        },
        confirmButton = {
            DestructiveDialogButton(
                text = stringResource(Res.string.delete),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(Res.string.cancel),
                onClick = state::hide,
            )
        },
    )
}

@Composable
private fun ModelActions(
    engineState: EngineState?,
    status: ModelStatus,
    downloadEnabled: Boolean,
    onLoad: (LoadedModel) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            is ModelStatus.Downloaded -> {
                ModelActionButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    ring = Ring.None,
                    onClick = onDeleteClick,
                )
                LoadButton(
                    engineState = engineState,
                    loadedModel = status.loadedModel,
                    onLoad = onLoad,
                )
            }
            is ModelStatus.Pending -> when (val taskStatus = status.taskStatus) {
                TaskStatus.NotFound -> ModelActionButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = stringResource(Res.string.download),
                    tint = MaterialTheme.colorScheme.primary,
                    ring = Ring.None,
                    enabled = downloadEnabled,
                    onClick = onDownload,
                )
                TaskStatus.Created -> ModelActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    ring = Ring.Indeterminate,
                    onClick = onCancel,
                )
                is TaskStatus.Running -> ModelActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    ring = when (val fraction = taskStatus.progress?.fraction) {
                        null -> Ring.Indeterminate
                        else -> Ring.Determinate(fraction)
                    },
                    onClick = onCancel,
                )
                is TaskStatus.Failed -> ModelActionButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = stringResource(Res.string.retry),
                    tint = MaterialTheme.colorScheme.error,
                    ring = Ring.None,
                    enabled = downloadEnabled,
                    onClick = onDownload,
                )
            }
        }
    }
}

@Composable
private fun LoadButton(
    engineState: EngineState?,
    loadedModel: LoadedModel,
    onLoad: (LoadedModel) -> Unit,
) {
    val matches = engineState?.modelName == loadedModel.model.name
    val descriptionRes = when {
        !matches -> Res.string.load
        engineState is EngineState.Ready -> Res.string.loaded
        engineState is EngineState.Loading -> Res.string.loading
        engineState is EngineState.Error -> Res.string.load_error
        else -> Res.string.load
    }
    ModelActionButton(
        icon = when {
            matches && engineState is EngineState.Ready -> Icons.Rounded.Check
            else -> Icons.Rounded.ModeStandby
        },
        contentDescription = stringResource(descriptionRes),
        tint = when {
            matches && engineState is EngineState.Ready -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        ring = when {
            matches && engineState is EngineState.Loading -> Ring.Indeterminate
            else -> Ring.None
        },
        onClick = { onLoad(loadedModel) },
    )
}

/**
 * An [AppBarIconButton] whose icon and [tint] convey a single model action, optionally wrapped by
 * a [CircularProgressIndicator] [ring] so in-flight downloads/loads show progress around the button.
 * The [Box] is a fixed size (button + ring gap) so appearing/disappearing rings don't shift layout.
 */
@Composable
private fun ModelActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    ring: Ring,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(UiTokens.appBarButtonSize + 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (ring) {
            Ring.None -> Unit
            Ring.Indeterminate -> CircularProgressIndicator(
                modifier = Modifier.matchParentSize(),
            )
            is Ring.Determinate -> CircularProgressIndicator(
                modifier = Modifier.matchParentSize(),
                // let is needed bc of compose lint about method references
                progress = animateFloatAsState(ring.fraction).let { state ->
                    state::value
                },
            )
        }
        FilledTonalIconButton(
            enabled = enabled,
            onClick = onClick,
            content = {
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    content = { targetIcon ->
                        Icon(
                            imageVector = targetIcon,
                            contentDescription = contentDescription,
                            tint = tint,
                        )
                    },
                )
            },
        )
    }
}

private val EngineState.modelName: String?
    get() = when (this) {
        is EngineState.Loading -> model.model.name
        is EngineState.Ready -> model.model.name
        is EngineState.Error -> model.model.name
        EngineState.Uninitialized -> null
    }

private sealed interface Ring {
    data object None : Ring
    data object Indeterminate : Ring
    data class Determinate(
        val fraction: Float,
    ) : Ring
}

@Composable
private fun MetadataChip(
    icon: ImageVector,
    text: String,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
