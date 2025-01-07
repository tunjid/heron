package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.runtime.Composable
import com.tunjid.heron.timeline.ui.post.PostFeatureTextContent

@Composable
internal fun BlockedPostPost(onClick: (() -> Unit)?) {
    FeatureContainer(onClick = onClick) {
        PostFeatureTextContent(
            title = "Post is blocked",
            description = "You are blocked from reading this post.",
            uri = null,
        )
    }
}
