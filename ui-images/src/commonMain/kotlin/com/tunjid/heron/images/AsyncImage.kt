package com.tunjid.heron.images

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.tunjid.composables.ui.animate
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.shapes.animate
import coil3.compose.AsyncImage as CoilAsyncImage

data class ImageArgs(
    val url: String?,
    val contentDescription: String? = null,
    val contentScale: ContentScale,
    val shape: RoundedPolygonShape,
)

@Composable
fun AsyncImage(
    args: ImageArgs,
    modifier: Modifier = Modifier,
) {
    CoilAsyncImage(
        modifier = modifier.clip(args.shape.animate()),
        model = args.url,
        contentDescription = args.contentDescription,
        contentScale = args.contentScale.animate()
    )
}