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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.tunjid.composables.ui.animate
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.files.uiDisplayModel
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

sealed interface Image {
    val size: IntSize
    val painter: Painter
}

interface ImageLoader {
    suspend fun fetchImage(
        request: ImageRequest,
        size: IntSize,
    ): Image?

    fun download(
        request: ImageRequest.Network,
    ): Flow<DownloadStatus>
}

sealed class ImageRequest {
    data class Network(
        val url: String?,
        val thumbnailUrl: String? = null,
    ) : ImageRequest()

    internal data class Local(
        val file: Any,
    ) : ImageRequest()
}

sealed class DownloadStatus {
    data object Failed : DownloadStatus()
    data object Indeterminate : DownloadStatus()
    data object Complete : DownloadStatus()
    data class Progress(
        val fraction: Float,
    ) : DownloadStatus()
}

data class ImageArgs(
    val request: ImageRequest,
    val contentDescription: String? = null,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val shape: RoundedPolygonShape,
)

@Stable
class ImageState internal constructor(
    args: ImageArgs,
    private val imageLoader: ImageLoader,
    private val windowSize: () -> IntSize,
) {
    var args by mutableStateOf(args)
    val imageSize
        get() = when (val currentImage = image) {
            null -> IntSize.Zero
            else -> currentImage.size
        }

    internal var image by mutableStateOf<Image?>(null)
    private var layoutSize by mutableStateOf(IntSize.Zero)

    internal fun layoutImage(
        measureScope: MeasureScope,
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult = with(measureScope) {
        val imageConstraints = constraints.copy(
            minWidth = when {
                constraints.hasBoundedWidth -> constraints.maxWidth
                else -> constraints.minWidth
            },
            minHeight = when {
                constraints.hasBoundedHeight -> constraints.maxHeight
                else -> constraints.minHeight
            },
        )
        layoutSize = IntSize(
            width = imageConstraints.minWidth,
            height = imageConstraints.minHeight,
        )
        val placeable = measurable.measure(imageConstraints)
        return layout(
            width = placeable.width,
            height = placeable.height,
        ) {
            placeable.place(0, 0)
        }
    }

    internal suspend fun loadImagesForLayoutSize() {
        combine(
            requests(),
            layoutSizes(),
            ::Pair,
        )
            .distinctUntilChanged()
            .collectLatest { (request, size) ->
                imageLoader.fetchImage(
                    request = request,
                    size = IntSize(
                        width = min(size.width, windowSize().width),
                        height = min(size.height, windowSize().height),
                    ),
                )
                    ?.let(::image::set)
            }
    }

    private fun requests(): Flow<ImageRequest> =
        snapshotFlow { args.request }

    private fun layoutSizes(): Flow<IntSize> =
        snapshotFlow { layoutSize }
            .filter { it.isUsable }
            .withIndex()
            .debounce { (index) ->
                if (index == 0) 0.milliseconds
                else ImageLayoutSizeRefetchDebounce.milliseconds
            }
            .map { it.value }
}

@Composable
fun rememberUpdatedImageState(
    args: ImageArgs,
): ImageState {
    val imageLoader = LocalImageLoader.current
    val windowSize = rememberUpdatedState(LocalWindowInfo.current.containerSize)
    return remember(imageLoader) {
        ImageState(
            args = args,
            imageLoader = imageLoader,
            windowSize = windowSize::value,
        )
    }
        .also { it.args = args }
}

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
        thumbnailUrl = thumbnailUrl,
    ),
    contentDescription = contentDescription,
    contentScale = contentScale,
    alignment = alignment,
    shape = shape,
)

fun ImageArgs(
    item: RestrictedFile.Media.Photo,
    contentDescription: String? = null,
    contentScale: ContentScale,
    alignment: Alignment = Alignment.Center,
    shape: RoundedPolygonShape,
) = ImageArgs(
    request = ImageRequest.Local(
        file = item.uiDisplayModel,
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

    val contentDescription = state.args.contentDescription
    val contentScale = state.args.contentScale.animate()
    val alignment = state.args.alignment.animate()
    val shape = state.args.shape.animate()

    Box(
        modifier = modifier
            .layout(state::layoutImage)
            .clip(shape),
    ) {
        val painter = remember {
            ImagePainter(state::image) { contentScale }
        }

        Image(
            modifier = Modifier
                .fillMaxSize(),
            painter = painter,
            contentDescription = contentDescription,
            alignment = alignment,
            contentScale = contentScale,
        )

        state.image?.AnimationEffect()
    }

    val scope = rememberCoroutineScope(
        Dispatchers.Main::immediate,
    )
    DisposableEffect(scope) {
        val job = scope.launch {
            state.loadImagesForLayoutSize()
        }
        onDispose(job::cancel)
    }
}

@Composable
internal fun Image.AnimationEffect() {
    when (this) {
        is CoilImage -> image.AnimationEffect()
    }
}

private val IntSize.isUsable: Boolean
    get() = width > IntSize.Zero.width &&
        width < Int.MAX_VALUE &&
        height > IntSize.Zero.height &&
        height < Int.MAX_VALUE

private const val ImageLayoutSizeRefetchDebounce = 100
