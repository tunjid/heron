package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.runtime.Composable
import com.tunjid.heron.timeline.ui.post.PostFeatureTextContent

@Composable
internal fun InvisiblePostPost(onClick: (() -> Unit)?) {
    FeatureContainer(onClick = onClick) {
        PostFeatureTextContent(
            title = "Post not found",
            description = "The post may have been deleted.",
            uri = null,
        )
    }
}
