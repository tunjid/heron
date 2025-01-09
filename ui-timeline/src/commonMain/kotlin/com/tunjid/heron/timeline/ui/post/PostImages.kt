package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.aspectRatio
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostImages(
    feature: ImageList,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    onImageClicked: (Int) -> Unit,
) {
    LazyRow(
        horizontalArrangement = spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = feature.images,
            key = { _, item -> item.thumb.uri },
            itemContent = { index, image ->
                val aspectRatio = if (!image.aspectRatio.isNaN()) image.aspectRatio else 1f
                sharedElementScope.updatedMovableSharedElementOf(
                    modifier = when (feature.images.size) {
                        1 -> Modifier
                            .fillParentMaxWidth()
                            .aspectRatio(aspectRatio)

                        else -> Modifier
                            .height(200.dp)
                            .aspectRatio(aspectRatio)
                    }
                        .clickable { onImageClicked(index) },
                    key = image.sharedElementKey(
                        prefix = sharedElementPrefix
                    ),
                    state = ImageArgs(
                        url = image.thumb.uri,
                        contentDescription = image.alt,
                        contentScale = ContentScale.Fit,
                        shape = RoundedCornerShape(16.dp).toRoundedPolygonShape()
                    ),
                    sharedElement = { state, innerModifier ->
                        AsyncImage(
                            modifier = innerModifier,
                            args = state,
                        )
                    }
                )
            }
        )
    }
}

fun Image.sharedElementKey(
    prefix: String,
) = "$prefix-${thumb.uri}"
