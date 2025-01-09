package com.tunjid.heron.scaffold.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onStarted: () -> Unit = {},
    onProgressed: (Float) -> Unit = {},
    onCancelled: () -> Unit = {},
    onBack: () -> Unit,
)
