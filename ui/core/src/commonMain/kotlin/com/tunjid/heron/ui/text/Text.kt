package com.tunjid.heron.ui.text

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow

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

@Composable
fun BoldedText(
    modifier: Modifier = Modifier,
    text: String,
    ellipsize: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = if (ellipsize) 1 else Int.MAX_VALUE,
        style = LocalTextStyle.current.copy(fontWeight = Bold),
    )
}

@Composable
fun SmallOutlinedText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}
