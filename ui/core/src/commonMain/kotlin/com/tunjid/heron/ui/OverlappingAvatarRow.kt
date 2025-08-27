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

package com.tunjid.heron.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
fun OverlappingAvatarRow(
    modifier: Modifier = Modifier,
    overlap: Dp,
    maxItems: Int,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->

        val overlapOffset = overlap.roundToPx()
        val totalWidth = constraints.maxWidth + (overlapOffset * (maxItems - 1))
        val itemSize = totalWidth / maxItems

        val placeables = measurables.map { measurable ->
            measurable.measure(
                Constraints.fixed(itemSize, itemSize),
            )
        }

        layout(constraints.maxWidth, itemSize) {
            // Track the x co-ord we have placed children up to
            var xPosition = 0

            // Place children in the parent layout
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = xPosition - (overlapOffset * index),
                    y = 0,
                )

                xPosition += placeable.width
            }
        }
    }
}
