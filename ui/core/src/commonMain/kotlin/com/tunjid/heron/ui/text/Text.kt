package com.tunjid.heron.ui.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EmphasizedSingleLineOutlinedText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodyMediumEmphasized,
        maxLines = 1,
    )
}
