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

package com.tunjid.heron.timeline.ui.label

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.LabelerCollectionShape
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.RecordLayout
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.labeling_service_by
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Labeler(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    labeler: Labeler,
) = with(movableElementSharedTransitionScope) {
    RecordLayout(
        modifier = modifier,
        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
        title = labeler.creator.displayName ?: "",
        subtitle = stringResource(
            heron.ui.timeline.generated.resources.Res.string.labeling_service_by,
            labeler.creator.handle.id,
        ),
        description = labeler.creator.description,
        blurb = "",
        sharedElementPrefix = sharedElementPrefix,
        sharedElementType = labeler.uri,
        avatar = {
            val avatar = labeler.creator.avatar ?: BlueskyClouds
            AsyncImage(
                modifier = Modifier
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = labeler.avatarSharedElementKey(sharedElementPrefix),
                        ),
                    )
                    .size(44.dp),
                args = remember(avatar) {
                    ImageArgs(
                        url = avatar.uri,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        contentDescription = null,
                        shape = LabelerCollectionShape,
                    )
                },
            )
        },
    )
}

fun Label.Definition.locale(
    currentLanguageTag: String,
) = locales.list
    .firstOrNull { it.lang == currentLanguageTag }
    ?: locales.list
        .firstOrNull()
