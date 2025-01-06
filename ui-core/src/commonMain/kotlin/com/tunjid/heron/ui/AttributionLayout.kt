package com.tunjid.heron.ui

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttributionLayout(
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
    label: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = spacedBy(16.dp),
    ) {
        avatar()
        Column(Modifier.weight(1f)) {
            label()
        }
        action?.invoke()
    }
}