package com.tunjid.heron

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    com.tunjid.heron.scaffold.scaffold.App(
        appState = remember { createAppState() },
        modifier = Modifier.fillMaxSize(),
    )
}