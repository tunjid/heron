package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.runtime.Composable
import com.tunjid.heron.timeline.ui.post.PostFeatureTextContent

@Composable
internal fun UnknownPostPost(onClick: (() -> Unit)?) {
    FeatureContainer(onClick = onClick) {
        PostFeatureTextContent(
            title = "Unknown post",
            description = "This post cannot be viewed.",
            uri = null,
        )
    }
}
