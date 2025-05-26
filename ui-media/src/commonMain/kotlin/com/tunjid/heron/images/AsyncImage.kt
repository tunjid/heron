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

package com.tunjid.heron.images

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImagePainter
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import io.github.vinceglb.filekit.PlatformFile
import coil3.compose.AsyncImage as CoilAsyncImage
import io.github.vinceglb.filekit.coil.AsyncImage as FileAsyncImage

sealed class ImageRequest {
    data class Network(
        val url: String?,
        val thumbnailUrl: String? = null,
    ) : ImageRequest()

    data class Local(
        val file: PlatformFile,
    ) : ImageRequest()
}

data class ImageArgs(
    val request: ImageRequest,
    val contentDescription: String? = null,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val shape: RoundedPolygonShape,
)

@Stable
class ImageState(
    args: ImageArgs,
) {
    var args by mutableStateOf(args)
    var imageSize by mutableStateOf(IntSize.Zero)
        private set

    internal fun updateFromSuccess(
        success: AsyncImagePainter.State.Success,
    ) {
        imageSize = IntSize(
            width = success.result.image.width,
            height = success.result.image.height,
        )
    }
}

@Composable
fun rememberUpdatedImageState(
    args: ImageArgs,
): ImageState =
    remember { ImageState(args) }
        .also { it.args = args }


fun ImageArgs(
    url: String?,
    thumbnailUrl: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale,
    alignment: Alignment = Alignment.Center,
    shape: RoundedPolygonShape,
) = ImageArgs(
    request = ImageRequest.Network(
        url = url,
        thumbnailUrl = thumbnailUrl
    ),
    contentDescription = contentDescription,
    contentScale = contentScale,
    alignment = alignment,
    shape = shape,
)

fun ImageArgs(
    file: PlatformFile,
    contentDescription: String? = null,
    contentScale: ContentScale,
    alignment: Alignment = Alignment.Center,
    shape: RoundedPolygonShape,
) = ImageArgs(
    request = ImageRequest.Local(
        file = file,
    ),
    contentDescription = contentDescription,
    contentScale = contentScale,
    alignment = alignment,
    shape = shape,
)

@Composable
fun AsyncImage(
    args: ImageArgs,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        state = rememberUpdatedImageState(args),
        modifier = modifier,
    )
}

@Composable
fun AsyncImage(
    state: ImageState,
    modifier: Modifier = Modifier,
) {
    val initialState = remember { state }
    check(state == initialState) {
        """
            ImageState must not change throughout the composition of AsyncImage, rather its
            mutable properties should be updated.
        """.trimIndent()
    }

    val args = state.args
    Box(
        modifier = modifier
            .clip(args.shape.animate())
    ) {
        val contentScale = args.contentScale.animate()

        when (val request = args.request) {
            is ImageRequest.Local -> {
                FileAsyncImage(
                    modifier = Modifier.fillMaxConstraints(),
                    file = request.file,
                    contentDescription = args.contentDescription,
                    contentScale = contentScale,
                    onSuccess = state::updateFromSuccess,
                )
            }

            is ImageRequest.Network -> {
                var thumbnailVisible by remember(request.thumbnailUrl) {
                    mutableStateOf(request.thumbnailUrl != null)
                }
                CoilAsyncImage(
                    modifier = Modifier.fillMaxConstraints(),
                    model = request.url,
                    contentDescription = args.contentDescription,
                    contentScale = contentScale,
                    onSuccess = { thumbnailVisible = false }
                )
                if (thumbnailVisible) CoilAsyncImage(
                    modifier = Modifier.fillMaxConstraints(),
                    model = request.thumbnailUrl,
                    contentDescription = args.contentDescription,
                    contentScale = contentScale,
                    onSuccess = state::updateFromSuccess,
                )
            }
        }
    }
}

private fun Modifier.fillMaxConstraints() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = when {
                    constraints.hasBoundedWidth -> constraints.maxWidth
                    else -> constraints.minWidth
                },
                minHeight = when {
                    constraints.hasBoundedHeight -> constraints.maxHeight
                    else -> constraints.minHeight
                }
            )
        )
        layout(
            width = placeable.width,
            height = placeable.height
        ) {
            placeable.place(0, 0)
        }
    }