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

package com.tunjid.heron.compose.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.MediaItem
import de.cketti.codepoints.codePointCount
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlin.math.max

import kotlin.math.min

@Composable
internal fun ComposePostBottomBar(
    postText: TextFieldValue,
    modifier: Modifier = Modifier,
    photos: List<MediaItem.Photo>,
    onMediaEdited: (Action.EditMedia) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imagePickerLauncher = rememberFilePickerLauncher(
            type = FileKitType.Image,
            mode = FileKitMode.Multiple(
                maxItems = max(
                    a = 0,
                    b = PhotoUploadLimit - photos.size,
                ).takeUnless(0::equals)
            )
        ) { images ->
            images
                ?.let(Action.EditMedia::AddPhotos)
                ?.let(onMediaEdited)
        }

        val videoPickerLauncher = rememberFilePickerLauncher(
            type = FileKitType.Video,
            mode = FileKitMode.Single
        ) { video ->
            video
                ?.let(Action.EditMedia::AddVideo)
                ?.let(onMediaEdited)
        }

        TabIcons.forEachIndexed { index, imageVector ->
            IconButton(
                onClick = {
                    when (index) {
                        0 -> if (photos.size < PhotoUploadLimit) imagePickerLauncher.launch()
                        1 -> videoPickerLauncher.launch()
                    }
                },
                content = {
                    Icon(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = when (index) {
                                    0 -> if (photos.size < PhotoUploadLimit) 1f else 0.6f
                                    else -> 1f
                                }
                            },
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = FloatingActionButtonDefaults.containerColor
                    )
                }
            )
        }
        Spacer(Modifier.weight(1f))
        PostTextLimit(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            postText = postText
        )
    }
}

@Composable
fun PostTextLimit(
    modifier: Modifier = Modifier,
    postText: TextFieldValue,
) {
    val postByteCount = postText.text.codePointCount(0, postText.text.length)
    val unboundedProgress = postByteCount / PostTextLimit.toFloat()

    Row(
        modifier = modifier,
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = (PostTextLimit - postByteCount).toString(),
        )

        val progress = min(1f, unboundedProgress)
        val easing = remember { CubicBezierEasing(.42f, 0f, 1f, 0.58f) }

        CircularProgressIndicator(
            modifier = Modifier.requiredSize(24.dp),
            progress = { progress },
            strokeWidth = lerp(
                start = 2.dp,
                stop = 8.dp,
                fraction = ((unboundedProgress - 1) * 4).coerceIn(0f, 1f),
            ),
            color = lerp(
                start = MaterialTheme.colorScheme.primary,
                stop = Color.Red,
                fraction = easing.transform(progress),
            )
        )
    }
}

private const val PostTextLimit = 300
private const val PhotoUploadLimit = 4

private val TabIcons = listOf(
    Icons.Rounded.Photo,
    Icons.Rounded.Movie,
)
