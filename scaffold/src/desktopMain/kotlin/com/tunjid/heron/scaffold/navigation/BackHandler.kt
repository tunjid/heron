package com.tunjid.heron.scaffold.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onStarted: () -> Unit,
    onProgressed: (Float) -> Unit,
    onCancelled: () -> Unit,
    onBack: () -> Unit
) = Unit