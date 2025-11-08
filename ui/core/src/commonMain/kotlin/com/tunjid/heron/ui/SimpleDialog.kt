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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SimpleDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
fun SimpleDialogTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun SimpleDialogText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun DestructiveDialogButton(
    text: String,
    onClick: () -> Unit,
) {
    StyledDialogButton(
        text = text,
        color = MaterialTheme.colorScheme.error,
        onClick = onClick,
    )
}

@Composable
fun NeutralDialogButton(
    text: String,
    onClick: () -> Unit,
) {
    StyledDialogButton(
        text = text,
        color = MaterialTheme.colorScheme.outline,
        onClick = onClick,
    )
}

@Composable
fun PrimaryDialogButton(
    text: String,
    onClick: () -> Unit,
) {
    StyledDialogButton(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick,
    )
}

@Composable
private fun StyledDialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = {
            onClick()
        },
        content = {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}
