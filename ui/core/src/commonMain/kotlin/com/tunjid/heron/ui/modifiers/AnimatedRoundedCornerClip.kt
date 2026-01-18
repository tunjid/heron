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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

fun Modifier.animatedRoundedCornerClip(
    cornerRadius: Dp,
): Modifier = this then AnimatedRoundedCornerClipElement(cornerRadius)

private data class AnimatedRoundedCornerClipElement(
    val cornerRadius: Dp,
) : ModifierNodeElement<AnimatedRoundedCornerClipNode>() {

    override fun create(): AnimatedRoundedCornerClipNode =
        AnimatedRoundedCornerClipNode(cornerRadius)

    override fun update(node: AnimatedRoundedCornerClipNode) {
        node.update(cornerRadius)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animatedClip"
        properties["isRounded"] = cornerRadius
    }
}

// 2. The Node: Holds state (Animatable) and logic (Draw)
private class AnimatedRoundedCornerClipNode(
    var cornerRadius: Dp,
) : Modifier.Node(),
    DrawModifierNode {

    // We replace animateDpAsState with Animatable.
    // We initialize it immediately based on the initial 'isRounded' value.
    private val clipRadius = Animatable(
        initialValue = cornerRadius,
        typeConverter = Dp.VectorConverter,
    )

    private val path = Path()

    fun update(
        targetRadius: Dp,
    ) {
        if (cornerRadius == targetRadius) return

        cornerRadius = targetRadius

        coroutineScope.launch {
            clipRadius.animateTo(targetRadius)
        }
    }

    override fun ContentDrawScope.draw() {
        // Reading clipRadius.value here automatically records a state read.
        // When the animation updates the value, this draw block will re-execute.
        val radiusPx = clipRadius.value.toPx()

        if (radiusPx <= 0f) {
            clipRect {
                this@draw.drawContent()
            }
            return
        }

        path.rewind()
        path.addRoundRect(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                cornerRadius = CornerRadius(radiusPx),
            ),
        )
        clipPath(path) {
            this@draw.drawContent()
        }
    }
}
