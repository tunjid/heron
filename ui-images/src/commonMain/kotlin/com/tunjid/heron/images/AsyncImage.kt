package com.tunjid.heron.images

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.composables.ui.animate
import coil3.compose.AsyncImage as CoilAsyncImage

data class ImageArgs(
    val url: String?,
    val description: String? = null,
    val contentScale: ContentScale,
)

@Composable
fun AsyncImage(
    args: ImageArgs,
    modifier: Modifier = Modifier,
) {
    CoilAsyncImage(
        modifier = modifier,
        model = args.url,
        contentDescription = args.description,
        contentScale = args.contentScale.animate()
    )
}