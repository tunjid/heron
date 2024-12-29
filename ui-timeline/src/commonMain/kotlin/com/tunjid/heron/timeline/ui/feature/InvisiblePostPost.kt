package com.tunjid.heron.timeline.ui.feature

import androidx.compose.runtime.Composable

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
