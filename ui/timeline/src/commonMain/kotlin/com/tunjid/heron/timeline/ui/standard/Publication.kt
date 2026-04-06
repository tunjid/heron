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

package com.tunjid.heron.timeline.ui.standard

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.DocumentCollectionShape
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout

@Composable
fun Publication(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    publication: StandardPublication,
) {
    RecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = publication.name,
        subtitle = publication.url,
        description = publication.description,
        blurb = null,
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = publication.uri,
        avatar = {
            publication.icon?.let { icon ->
                AsyncImage(
                    modifier = Modifier.size(44.dp),
                    args = remember(icon) {
                        ImageArgs(
                            url = icon.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            shape = DocumentCollectionShape,
                        )
                    },
                )
            }
        },
    )
}
