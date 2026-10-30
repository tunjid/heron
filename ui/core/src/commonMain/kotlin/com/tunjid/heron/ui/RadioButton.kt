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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val light by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MatteLightAnimationSpec,
    )
    val colors = rememberMatteColors()

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(
                if (onClick != null) Modifier.selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = null,
                )
                else Modifier,
            )
            .graphicsLayer {
                alpha = if (enabled) 1f else MatteDisabledAlpha
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RadioButtonSize)
                .radioButtonSocket(
                    colors = colors,
                    light = { light },
                ),
        )
    }
}

/**
 * A round socket etched into the surface, the same as the [Selector]'s, whose floor lights up
 * when selected.
 */
private fun Modifier.radioButtonSocket(
    colors: MatteColors,
    light: () -> Float,
) = drawWithCache {
    val socket = matteChamfer(
        colors = colors,
        chamferSize = size,
    )
    onDrawBehind {
        socket(
            lerp(
                start = colors.socket,
                stop = colors.track,
                fraction = light().coerceIn(0f, 1f),
            ),
        )
    }
}

private val RadioButtonSize = MatteThumbSize + (MatteSocketClearance * 2)
