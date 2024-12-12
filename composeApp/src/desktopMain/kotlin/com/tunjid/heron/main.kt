package com.tunjid.heron

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tunjid.heron.scaffold.scaffold.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize(450.dp, 800.dp)
        ),
        title = "heron",
    ) {
        App(
            appState = remember { createAppState() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}