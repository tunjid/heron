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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = MatteThumbAnimationSpec,
    )
    val colors = rememberMatteColors()

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
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
                .size(
                    width = SwitchWidth,
                    height = MatteTrackThickness,
                )
                .switchTrack(
                    colors = colors,
                    floor = {
                        lerp(
                            start = colors.socket,
                            stop = colors.track,
                            fraction = progress.coerceIn(0f, 1f),
                        )
                    },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        val inset = MatteTrackInset.toPx()
                        val travel = SwitchWidth.toPx() - MatteThumbSize.toPx() - (inset * 2)
                        IntOffset(
                            x = (inset + (travel * progress)).fastRoundToInt(),
                            y = 0,
                        )
                    }
                    .size(MatteThumbSize)
                    .matteThumb(
                        colors = colors,
                        color = {
                            lerp(
                                start = colors.mutedThumb,
                                stop = colors.thumb,
                                fraction = progress.coerceIn(0f, 1f),
                            )
                        },
                    ),
            )
        }
    }
}

private fun Modifier.switchTrack(
    colors: MatteColors,
    floor: () -> Color,
) = drawWithCache {
    val groove = matteChamfer(
        colors = colors,
        chamferSize = size,
    )
    onDrawBehind {
        groove(floor())
    }
}

private val SwitchWidth = 56.dp
