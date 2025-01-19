package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal inline fun FeatureContainer(
    noinline onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    val shape = remember { RoundedCornerShape(8.dp) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = shape,
            )
            .then(modifier)
            .then(clickableModifier),
    ) {
        content()
    }
}
