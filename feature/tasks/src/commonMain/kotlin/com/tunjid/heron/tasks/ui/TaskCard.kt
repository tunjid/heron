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

package com.tunjid.heron.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.Publication
import com.tunjid.heron.timeline.utilities.EmbeddedRecord
import com.tunjid.heron.timeline.utilities.roundComponent
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.text.message
import heron.feature.tasks.generated.resources.Res
import heron.feature.tasks.generated.resources.dismiss
import heron.feature.tasks.generated.resources.failed_at
import heron.feature.tasks.generated.resources.failed_reason_io
import heron.feature.tasks.generated.resources.media_photos_multiple
import heron.feature.tasks.generated.resources.media_photos_single
import heron.feature.tasks.generated.resources.media_videos_multiple
import heron.feature.tasks.generated.resources.media_videos_single
import heron.feature.tasks.generated.resources.retry
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InFlightTaskCard(
    modifier: Modifier = Modifier,
    item: TaskItem.InFlight,
    paneTransitionScope: PaneTransitionScope,
) {
    TaskCard(
        modifier = modifier,
        item = item,
        paneTransitionScope = paneTransitionScope,
    )
}

@Composable
internal fun FailedTaskCard(
    modifier: Modifier = Modifier,
    item: TaskItem.Failed,
    paneTransitionScope: PaneTransitionScope,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val relativeTime = remember(item.failedWrite.failedAt) {
        (Clock.System.now() - item.failedWrite.failedAt).roundComponent()
    }
    TaskCard(
        modifier = modifier,
        item = item,
        paneTransitionScope = paneTransitionScope,
        footer = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.failed_at, relativeTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (item.failedWrite.reason == FailedWrite.Reason.IO) Text(
                    text = stringResource(Res.string.failed_reason_io),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.dismiss))
                    }
                    OutlinedButton(onClick = onRetry) {
                        Text(text = stringResource(Res.string.retry))
                    }
                }
            }
        },
    )
}

@Composable
private fun TaskCard(
    modifier: Modifier = Modifier,
    item: TaskItem,
    paneTransitionScope: PaneTransitionScope,
    footer: (@Composable () -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val description = item.description
            AttributionLayout(
                avatar = {
                    TaskIcon(icon = description.icon)
                },
                label = {
                    Text(
                        text = description.title.message,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val summary = description.summary
                    if (summary != null) Text(
                        text = summary.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val media = mediaSummary(
                        photoCount = description.photoCount,
                        videoCount = description.videoCount,
                    )
                    if (media != null) Text(
                        text = media,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            item.associatedRecord?.let { record ->
                TaskEmbeddedRecord(
                    record = record,
                    paneTransitionScope = paneTransitionScope,
                )
            }
            footer?.invoke()
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TaskEmbeddedRecord(
    record: Record.Embeddable,
    paneTransitionScope: PaneTransitionScope,
) {
    // A never matched prefix; these records are read only and do not transition.
    val sharedElementPrefix = remember { Uuid.random().toString() }
    when (record) {
        is Record.Embeddable.Native -> EmbeddedRecord(
            modifier = Modifier,
            record = record,
            appliedLabels = AppliedLabels.Empty,
            sharedElementPrefix = sharedElementPrefix,
            paneTransitionScope = paneTransitionScope,
            postActions = PostActions.NoOp,
        )
        is StandardDocument -> OutlinedCard {
            Document(
                modifier = Modifier.padding(12.dp),
                paneTransitionScope = paneTransitionScope,
                sharedElementPrefix = sharedElementPrefix,
                document = record,
                onPublicationClicked = null,
                onSubscriptionToggled = null,
            )
        }
        is StandardPublication -> OutlinedCard {
            Publication(
                modifier = Modifier.padding(12.dp),
                paneTransitionScope = paneTransitionScope,
                sharedElementPrefix = sharedElementPrefix,
                publication = record,
                onSubscriptionToggled = { _, _ -> },
            )
        }
    }
}

@Composable
private fun TaskIcon(
    icon: ImageVector,
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun mediaSummary(
    photoCount: Int,
    videoCount: Int,
): String? {
    val parts = buildList {
        if (photoCount > 0) add(
            stringResource(
                if (photoCount == 1) Res.string.media_photos_single
                else Res.string.media_photos_multiple,
                photoCount,
            ),
        )
        if (videoCount > 0) add(
            stringResource(
                if (videoCount == 1) Res.string.media_videos_single
                else Res.string.media_videos_multiple,
                videoCount,
            ),
        )
    }
    return parts.takeIf(List<String>::isNotEmpty)?.joinToString(separator = " · ")
}
