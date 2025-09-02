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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import io.github.vinceglb.filekit.PlatformFile
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

sealed interface Image {
    val size: IntSize

    fun drawWith(scope: DrawScope)
}

interface ImageLoader {
    suspend fun fetchImage(
        request: ImageRequest,
        size: IntSize,
    ): Image?
}

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
    internal var layoutSize by mutableStateOf(IntSize.Zero)

    internal suspend fun loadImagesForLayoutSize() {
        snapshotFlow { layoutSize }
            .filter { it.isUsable }
            .distinctUntilChanged()
            .withIndex()
            .debounce { (index) ->
                if (index == 0) 0.milliseconds
                else ImageLayoutSizeRefetchDebounce.milliseconds
            }
            .collectLatest { (_, size) ->
                imageLoader.fetchImage(
                    request = args.request,
                    size = IntSize(
                        width = min(size.width, windowSize().width),
                        height = min(size.height, windowSize().height),
                    ),
                )
                    ?.let(::image::set)
            }
    }
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

    Box(
        modifier = modifier
            .clip(state.args.shape.animate()),
    ) {
        val contentScale = state.args.contentScale.animate()
        AnimatedContent(
            modifier = Modifier
                .fillMaxConstraints {
                    state.layoutSize = it
                },
            targetState = state.image,
            transitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
        ) { image ->
            if (image != null) Box(
                modifier = Modifier
                    .drawBehind {
                        scaleAndAlignTo(
                            srcSize = image.size,
                            destSize = size.roundToIntSize(),
                            contentScale = contentScale,
                            alignment = Alignment.Center,
                            block = image::drawWith,
                        )
                    },
            )
        }
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

private inline fun DrawScope.scaleAndAlignTo(
    srcSize: IntSize,
    destSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
    crossinline block: DrawScope.() -> Unit,
) {
    val scaleFactor = contentScale.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize(),
    )

    val alignmentOffset = alignment.align(
        size = srcSize,
        space = destSize,
        layoutDirection = layoutDirection,
    )

    val translationOffset = Offset(
        x = alignmentOffset.x * scaleFactor.scaleX,
        y = alignmentOffset.y * scaleFactor.scaleY,
    )

    translate(
        left = translationOffset.x,
        top = translationOffset.y,
        block = {
            scale(
                scaleX = scaleFactor.scaleX,
                scaleY = scaleFactor.scaleY,
                block = block,
            )
        },
    )
}

private val IntSize.isUsable: Boolean
    get() = width > IntSize.Zero.width &&
        width < Int.MAX_VALUE &&
        height > IntSize.Zero.height &&
        height < Int.MAX_VALUE

private const val ImageLayoutSizeRefetchDebounce = 100
