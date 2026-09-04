package com.tunjid.heron.timeline.ui.derakkuma

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.RockSkyCollectionShape
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.PaneTransitionScope

@Composable
internal fun PaneTransitionScope.DerakkumaAvatar(
    image: ImageUri?,
    uri: RecordUri,
    sharedElementPrefix: String?,
) {
    val resolved = image?.uri ?: "https://derakkuma.com/favicon.ico"
    PaneStickySharedElement(
        modifier = Modifier.size(44.dp),
        sharedContentState = rememberSharedContentState(
            key = uri.avatarSharedElementKey(sharedElementPrefix),
        ),
    ) {
        AsyncImage(
            modifier = Modifier.fillParentAxisIfFixedOrWrap(),
            args = remember(resolved) {
                ImageArgs(
                    url = resolved,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    shape = RockSkyCollectionShape,
                )
            },
        )
    }
}
