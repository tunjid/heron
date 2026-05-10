/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.timeline.ui.rocksky

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
import com.tunjid.heron.timeline.utilities.orDefault
import com.tunjid.heron.ui.PaneTransitionScope

@Composable
internal fun PaneTransitionScope.RockSkyAvatar(
    image: ImageUri?,
    uri: RecordUri,
    sharedElementPrefix: String?,
) {
    val resolved = image.orDefault
    PaneStickySharedElement(
        modifier = Modifier
            .size(44.dp),
        sharedContentState = rememberSharedContentState(
            key = uri.avatarSharedElementKey(sharedElementPrefix),
        ),
    ) {
        AsyncImage(
            modifier = Modifier
                .fillParentAxisIfFixedOrWrap(),
            args = remember(resolved) {
                ImageArgs(
                    url = resolved.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    shape = RockSkyCollectionShape,
                )
            },
        )
    }
}
