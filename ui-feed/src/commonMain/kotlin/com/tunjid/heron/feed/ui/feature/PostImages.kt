package com.tunjid.heron.feed.ui.feature

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.aspectRatio
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.toImageShape

@Composable
internal fun PostImages(
    feature: ImageList,
) {
    LazyRow(
        horizontalArrangement = spacedBy(8.dp),
    ) {
        items(
            items = feature.images,
            key = { it.thumb.uri },
            itemContent = { image ->
                val aspectRatio = if (!image.aspectRatio.isNaN()) image.aspectRatio else 1f
                AsyncImage(
                    modifier =
                    when (feature.images.size) {
                        1 -> Modifier
                            .fillParentMaxWidth()
                            .aspectRatio(aspectRatio)

                        else -> Modifier
                            .height(200.dp)
                            .aspectRatio(aspectRatio)
                    },
                    args = ImageArgs(
                        url = image.thumb.uri,
                        contentDescription = image.alt,
                        contentScale = ContentScale.Fit,
                        shape = RoundedCornerShape(16.dp).toImageShape()
                    )
                )
            }
        )
    }
}
