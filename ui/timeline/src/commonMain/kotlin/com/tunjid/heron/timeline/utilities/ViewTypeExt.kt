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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.modifiers.ifTrue

internal fun Modifier.sensitiveContentBlur(
    shape: Shape,
) = drawWithCache {
    val density = Density(density)
    val color = Color.Black.copy(alpha = 0.5f)
    onDrawWithContent {
        drawContent()
        drawOutline(
            outline = shape.createOutline(
                size = size,
                layoutDirection = layoutDirection,
                density = density,
            ),
            color = color,
        )
    }
}
    .blur(
        shape = shape,
        radius = ::SensitiveContentBlurRadius,
        clip = ::SensitiveContentBlurClip,
        progress = { 1f },
    )

@Composable
internal fun SensitiveContentBox(
    modifier: Modifier,
    isBlurred: Boolean,
    canUnblur: Boolean,
    label: String,
    icon: ImageVector?,
    onUnblurClicked: () -> Unit,
    content: @Composable (isBlurred: Boolean) -> Unit,
) {
    Box(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .ifTrue(
                        predicate = isBlurred,
                        block = Modifier::blockClickEvents,
                    ),
                content = {
                    content(isBlurred)
                },
            )

            if (isBlurred && canUnblur) {
                SensitiveContentButton(
                    modifier = Modifier
                        .align(Alignment.Center),
                    icon = icon,
                    label = label,
                    onClick = onUnblurClicked,
                )
            }
        },
    )
}

@Composable
private fun SensitiveContentButton(
    modifier: Modifier,
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = {
            onClick()
        },
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
                Text(
                    text = label,
                )
            }
        },
    )
}

private fun Modifier.blockClickEvents() =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            waitForUpOrCancellation(PointerEventPass.Initial)?.consume()
        }
    }

private val SensitiveContentBlurRadius = 120.dp
private const val SensitiveContentBlurClip = true
