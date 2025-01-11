package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.timeline.ui.post.PostFeatureTextContent

@Composable
internal fun UnknownPostPost(onClick: (() -> Unit)?) {
    FeatureContainer(
        modifier = Modifier.padding(16.dp),
        onClick = onClick,
    ) {
        PostFeatureTextContent(
            title = "Unknown post",
            description = "This post cannot be viewed.",
            uri = null,
        )
    }
}
