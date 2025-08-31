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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage as CoilAsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import io.github.vinceglb.filekit.PlatformFile
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
class ImageState internal constructor(
    args: ImageArgs,
    private val platformContext: PlatformContext,
    private val windowSize: () -> IntSize,
) {
    var args by mutableStateOf(args)
    var imageSize by mutableStateOf(IntSize.Zero)
        private set

    internal var layoutSize by mutableStateOf(IntSize.Zero)
    private var requestSize by mutableStateOf(IntSize.Zero)

    internal val request
        get() = imageRequest(requestSize)

    internal suspend fun updateRequest() {
        snapshotFlow { layoutSize }
            .filter { it.isUsable }
            .distinctUntilChanged()
            .debounce(ImageLayoutSizeRefetchDebounce.milliseconds)
            .collect {
                requestSize = it
            }
    }

    internal fun updateFromSuccess(
        success: AsyncImagePainter.State.Success,
    ) {
        imageSize = IntSize(
            width = success.result.image.width,
            height = success.result.image.height,
        )
    }

    private fun imageRequest(
        requestSize: IntSize
    ) = coil3.request.ImageRequest.Builder(platformContext).apply {
        when (val request = args.request) {
            is ImageRequest.Local -> {
                data(request.file)
            }
            is ImageRequest.Network -> {
                data(request.url)
                crossfade(true)
                request.thumbnailUrl
                    ?.let(MemoryCache::Key)
                    ?.let { cacheKey ->
                        placeholder {
                            SingletonImageLoader.get(platformContext)
                                .memoryCache
                                ?.get(cacheKey)
                                ?.image
                        }
                    }
                // TODO: This is only done for network images for now. This is bc
                // Local images need to be loaded as is to obtain the proper dimensions
                if (requestSize.isUsable) size(
                    CoilSize(
                        width = min(requestSize.width, windowSize().width),
                        height = min(requestSize.height, windowSize().height),
                    ),
                )
            }
        }
    }
        .build()
}

@Composable
fun rememberUpdatedImageState(
    args: ImageArgs,
): ImageState {
    val platformContext = LocalPlatformContext.current
    val windowSize = rememberUpdatedState(LocalWindowInfo.current.containerSize)
    return remember {
        ImageState(
            args = args,
            platformContext = platformContext,
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
            .clip(args.shape.animate()),
    ) {
        CoilAsyncImage(
            modifier = Modifier.fillMaxConstraints {
                state.layoutSize = it
            },
            model = state.request,
            contentDescription = state.args.contentDescription,
            contentScale = args.contentScale.animate(),
            onSuccess = state::updateFromSuccess,
        )
    }

    LaunchedEffect(Unit) {
        state.updateRequest()
    }
}

private fun Modifier.fillMaxConstraints(
    onConstraintsSized: (IntSize) -> Unit,
) = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = when {
                constraints.hasBoundedWidth -> constraints.maxWidth
                else -> constraints.minWidth
            },
            minHeight = when {
                constraints.hasBoundedHeight -> constraints.maxHeight
                else -> constraints.minHeight
            },
        ).also {
            onConstraintsSized(IntSize(it.minWidth, it.minHeight))
        },
    )
    layout(
        width = placeable.width,
        height = placeable.height,
    ) {
        placeable.place(0, 0)
    }
}

private val IntSize.isUsable: Boolean
    get() = width > IntSize.Zero.width &&
        width < Int.MAX_VALUE &&
        height > IntSize.Zero.height &&
        height < Int.MAX_VALUE

private const val ImageLayoutSizeRefetchDebounce = 100
