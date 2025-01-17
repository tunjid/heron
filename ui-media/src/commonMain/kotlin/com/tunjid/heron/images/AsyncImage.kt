package com.tunjid.heron.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import coil3.compose.AsyncImage as CoilAsyncImage

data class ImageArgs(
    val url: String?,
    val thumbnailUrl: String? = null,
    val contentDescription: String? = null,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val shape: RoundedPolygonShape,
)

@Composable
fun AsyncImage(
    args: ImageArgs,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val updatedThumbnail = rememberUpdatedState(args.thumbnailUrl)
    val platformContext = LocalPlatformContext.current
    val thumbnailRequest by remember {
        derivedStateOf {
            if (size == IntSize.Zero) return@derivedStateOf null
            val thumbnailUrl = updatedThumbnail.value ?: return@derivedStateOf null
            ImageRequest.Builder(platformContext)
                .data(thumbnailUrl)
                .size(size.width, size.height)
                .build()
        }
    }

    CoilAsyncImage(
        modifier = modifier
            .clip(args.shape.animate())
            .onSizeChanged { size = it },
        model = args.url,
        placeholder = thumbnailRequest?.let { rememberAsyncImagePainter(it) },
        contentDescription = args.contentDescription,
        contentScale = args.contentScale.animate()
    )
}