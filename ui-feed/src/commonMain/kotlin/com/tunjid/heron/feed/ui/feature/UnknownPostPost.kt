package com.tunjid.heron.feed.ui.feature

import androidx.compose.runtime.Composable
import com.tunjid.heron.feed.ui.feature.FeatureContainer
import com.tunjid.heron.feed.ui.feature.PostFeatureTextContent

@Composable
fun UnknownPostPost(onClick: (() -> Unit)?) {
    FeatureContainer(onClick = onClick) {
        PostFeatureTextContent(
            title = "Unknown post",
            description = "This post cannot be viewed.",
            uri = null,
        )
    }
}
