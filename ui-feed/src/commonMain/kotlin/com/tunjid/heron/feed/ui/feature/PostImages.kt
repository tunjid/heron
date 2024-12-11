package com.tunjid.heron.feed.ui.feature

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs

@Composable
internal fun PostImages(
    feature: ImageList,
) {
    Row(horizontalArrangement = spacedBy(8.dp)) {
        val modifier = if (feature.images.size == 1) {
            Modifier
        } else {
            Modifier
                .weight(1f)
                .aspectRatio(1f)
        }

        feature.images.forEachIndexed { i, image ->
            AsyncImage(
                modifier = modifier,
                args = ImageArgs(
                    url = image.thumb.uri,
                    description = image.alt,
                    contentScale = ContentScale.Crop,
                )
            )
        }
    }
}
