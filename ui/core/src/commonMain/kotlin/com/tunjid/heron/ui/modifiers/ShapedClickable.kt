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

package com.tunjid.heron.ui.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Applies the [clip] [Modifier] and the [clickable] [Modifier].
 * @param shape the clip [Shape].
 * @param onClick the click action.
 */
fun Modifier.shapedClickable(
    shape: Shape = DefaultClipShape,
    onClick: () -> Unit,
) =
    this
        .then(clipped(shape))
        .clickable(
            onClick = onClick,
        )

/**
 * Applies [shapedClickable] to the root of this [Modifier] chain.
 *
 * @see [shapedClickable]
 */
fun Modifier.rootShapedClickable(
    shape: Shape = DefaultClipShape,
    onClick: () -> Unit,
) = Modifier.shapedClickable(
    shape = shape,
    onClick = onClick,
)
    .then(this)

private fun clipped(shape: Shape): Modifier =
    if (shape == DefaultClipShape) DefaultClipModifier else Modifier.clip(shape)

private val DefaultClipShape = RoundedCornerShape(8.dp)

private val DefaultClipModifier = Modifier
    .clip(DefaultClipShape)
