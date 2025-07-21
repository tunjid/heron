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

package com.tunjid.heron.timeline.utilities

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

internal fun Modifier.presentationPadding(
    presentation: Timeline.Presentation,
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
) = when (presentation) {
    Timeline.Presentation.Text.WithEmbed -> padding(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
    )

    Timeline.Presentation.Media.Condensed -> this
    Timeline.Presentation.Media.Expanded -> this
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val FeedGeneratorCollectionShape = RoundedPolygonShape.Custom(
    polygon = MaterialShapes.Square,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ListCollectionShape = RoundedPolygonShape.Custom(
    polygon = MaterialShapes.Pill,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val StarterPackCollectionShape = RoundedPolygonShape.Custom(
    polygon = MaterialShapes.Cookie9Sided,
)