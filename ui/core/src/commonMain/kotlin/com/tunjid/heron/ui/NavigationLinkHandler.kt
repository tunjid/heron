package com.tunjid.heron.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import com.tunjid.heron.data.core.models.LinkTarget

@Composable
fun navigableLinkTargetHandler(
    onNavigableLinkTargetClicked: (LinkTarget.Navigable) -> Unit,
): (LinkTarget) -> Unit {
    val uriHandler = LocalUriHandler.current
    return remember(onNavigableLinkTargetClicked) {
        { linkTarget ->
            when (linkTarget) {
                is LinkTarget.ExternalLink -> runCatching {
                    uriHandler.openUri(linkTarget.uri.uri)
                }
                is LinkTarget.Navigable -> onNavigableLinkTargetClicked(
                    linkTarget,
                )
            }
        }
    }
}
