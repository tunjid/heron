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

package com.tunjid.heron.settings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.collapse_icon
import heron.feature.settings.generated.resources.expand_icon
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsItemRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .semantics {
                        contentDescription = title
                    }
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 8.dp,
                    ),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
        )
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Text(
            modifier = Modifier
                .weight(1f),
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        content()
    }
}

@Composable
fun ExpandableSettingsItemRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
            ),
    ) {
        SettingsItemRow(
            modifier = Modifier
                .fillMaxWidth(),
            title = title,
            icon = icon,
            titleColor = titleColor,
        ) {
            val iconRotation = animateFloatAsState(
                targetValue = if (isExpanded) 0f
                else 180f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            Icon(
                modifier = Modifier.graphicsLayer {
                    rotationX = iconRotation.value
                },
                imageVector = Icons.Default.ExpandLess,
                contentDescription = stringResource(
                    if (isExpanded) Res.string.collapse_icon
                    else Res.string.expand_icon,
                ),
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            visible = isExpanded,
            enter = EnterTransition,
            exit = ExitTransition,
            content = {
                Column(
                    modifier = Modifier
                        // Inset expanded content from the start to disambiguate
                        // it from other items
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                ) {
                    content()
                }
            },
        )
    }
}

@Composable
fun SettingsToggleItem(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .clickable { onCheckedChange(!checked) }
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = text,
        )
        Spacer(
            modifier = Modifier
                .width(16.dp),
        )
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private val EnterTransition = fadeIn() + slideInVertically { -it }
private val ExitTransition =
    shrinkOut { IntSize(it.width, 0) } + slideOutVertically { -it } + fadeOut()

private val SettingsItemShape = RoundedCornerShape(8.dp)
private val SettingsItemClipModifier = Modifier
    .clip(SettingsItemShape)
