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

import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline

// Public extensions
val Timeline.Presentation.cardSize
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 340.dp
        Timeline.Presentation.Media.Condensed -> 160.dp
        Timeline.Presentation.Media.Expanded -> 340.dp
        Timeline.Presentation.Media.Grid -> 120.dp
    }

val Timeline.Presentation.timelineHorizontalPadding
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Condensed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 0.dp
        Timeline.Presentation.Media.Grid -> 2.dp
    }

val Timeline.Presentation.lazyGridHorizontalItemSpacing
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Condensed -> 4.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Grid -> 2.dp
    }

val Timeline.Presentation.lazyGridVerticalItemSpacing
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Condensed -> 4.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Grid -> 2.dp
    }
