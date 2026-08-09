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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppBarIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
        colors = colors,
    ) {
        IconButton(
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(UiTokens.appBarButtonSize),
        ) {
            content()
        }
    }
}

@Composable
fun AppBarIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconDescription: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    onClick: () -> Unit,
) {
    val toolTipState = rememberTooltipState()
    val interactionSource = remember { MutableInteractionSource() }
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            spacingBetweenTooltipAndAnchor = 4.dp,
        ),
        tooltip = {
            PlainTooltip {
                Text(iconDescription)
            }
        },
        state = toolTipState,
    ) {
        AppBarIconButton(
            enabled = enabled,
            colors = colors,
            onClick = onClick,
            content = {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = tint,
                )
            },
        )
    }

    val pressedState = interactionSource.collectIsPressedAsState()
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    LaunchedEffect(
        pressedState,
        longPressTimeoutMillis,
    ) {
        snapshotFlow(pressedState::value)
            .collectLatest { pressed ->
                if (!pressed) return@collectLatest toolTipState.dismiss()

                delay(longPressTimeoutMillis.milliseconds)
                toolTipState.show()
            }
    }
}

@Composable
fun AppBarTextButton(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
        colors = colors,
        onClick = onClick,
    ) {
        AppBarRow(content)
    }
}

@Composable
fun AppBarElevatedCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    content: @Composable RowScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
        colors = colors,
    ) {
        AppBarRow(content)
    }
}

@Composable
private fun AppBarRow(
    content: @Composable (RowScope.() -> Unit),
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(UiTokens.appBarButtonSize),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
