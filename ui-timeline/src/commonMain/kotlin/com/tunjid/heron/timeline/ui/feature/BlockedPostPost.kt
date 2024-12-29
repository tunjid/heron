package com.tunjid.heron.timeline.ui.feature

import androidx.compose.runtime.Composable

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
