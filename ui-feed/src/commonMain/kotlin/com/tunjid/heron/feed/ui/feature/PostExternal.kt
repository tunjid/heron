package com.tunjid.heron.feed.ui.feature

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.toImageShape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostExternal(
    feature: ExternalEmbed,
    postId: Id,
    sharedElementPrefix: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onClick: () -> Unit,
) = with(sharedTransitionScope) {
    val uriHandler = LocalUriHandler.current
    FeatureContainer(
        modifier = Modifier.sharedElement(
            state = rememberSharedContentState(
                embedSharedElementKey(
                    prefix = sharedElementPrefix,
                    postId = postId,
                    text = feature.uri.uri,
                )
            ),
            animatedVisibilityScope = animatedVisibilityScope,
        ),
        onClick = onClick,
    ) {
        Row(horizontalArrangement = spacedBy(16.dp)) {
            if (!feature.thumb?.uri.isNullOrBlank()) {
                AsyncImage(
                    modifier = Modifier
                        .requiredSizeIn(
                            maxWidth = 96.dp,
                            maxHeight = 96.dp,
                        )
                        .sharedElement(
                            state = rememberSharedContentState(
                                embedSharedElementKey(
                                    prefix = sharedElementPrefix,
                                    postId = postId,
                                    text = feature.thumb?.uri,
                                )
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    args = ImageArgs(
                        url = feature.thumb?.uri,
                        contentDescription = feature.title,
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(16.dp).toImageShape()
                    ),
//          onClick = { uriHandler.openUri(feature.uri.uri) },
//          fallbackColor = MaterialTheme.colorScheme.outline,
                )
            }
            PostFeatureTextContent(
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        state = rememberSharedContentState(
                            embedSharedElementKey(
                                prefix = sharedElementPrefix,
                                postId = postId,
                                text = feature.title,
                            )
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                title = feature.title,
                description = feature.description,
                uri = feature.uri,
            )
        }
    }
}

@Composable
fun PostFeatureTextContent(
    modifier: Modifier = Modifier,
    title: String?,
    description: String?,
    uri: Uri?,
) {
    Column(modifier, verticalArrangement = spacedBy(4.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = Bold),
            )
        }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        val url = uri?.uri
        if (!url.isNullOrBlank()) {
            Text(
                text = url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun embedSharedElementKey(
    prefix: String,
    postId: Id,
    text: String?,
): String = "$prefix-${postId.id}-$text"