package com.tunjid.heron.feed.ui.feature

import androidx.compose.runtime.Composable

@Composable
fun InvisiblePostPost(onClick: (() -> Unit)?) {
  FeatureContainer(onClick = onClick) {
    PostFeatureTextContent(
      title = "Post not found",
      description = "The post may have been deleted.",
      uri = null,
    )
  }
}
