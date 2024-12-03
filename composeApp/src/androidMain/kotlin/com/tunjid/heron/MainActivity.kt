package com.tunjid.heron

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import com.tunjid.heron.scaffold.scaffold.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appViewModel by viewModels<AppViewModel>()
        val appState = appViewModel.appState

        setContent {
            App(
                appState = appState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val appState = createAppState(app)
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}