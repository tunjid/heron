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

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    FilterChip(
        modifier = modifier,
        shape = ChipShape,
        border = null,
        selected = selected,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedContainerColor = TabsState.TabBackgroundColor,
            iconColor = MaterialTheme.colorScheme.outline,
            labelColor = MaterialTheme.colorScheme.outline,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        onClick = onClick,
        leadingIcon = leadingIcon?.let { icon ->
            {
                ChipIcon(
                    imageVector = icon,
                )
            }
        },
        trailingIcon = trailingIcon?.let { icon ->
            {
                ChipIcon(
                    imageVector = icon,
                )
            }
        },
        label = {
            Text(
                text = text,
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun ChipIcon(
    imageVector: ImageVector,
) {
    Icon(
        modifier = Modifier
            .size(FilterChipDefaults.IconSize),
        imageVector = imageVector,
        contentDescription = null,
    )
}

private val ChipShape = RoundedCornerShape(16.dp)
