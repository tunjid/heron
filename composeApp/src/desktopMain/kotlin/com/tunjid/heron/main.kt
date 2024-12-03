package com.tunjid.heron

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tunjid.heron.scaffold.scaffold.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "heron",
    ) {
        App(
            appState = remember { createAppState() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}