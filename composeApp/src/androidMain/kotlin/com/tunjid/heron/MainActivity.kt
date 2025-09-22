/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.scaffold.scaffold.App
import com.tunjid.heron.scaffold.scaffold.isShowingSplashScreen
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {

    private val appState by lazy {
        (application as HeronApplication).appState
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition(appState::isShowingSplashScreen)
            setOnExitAnimationListener(SplashScreenViewProvider::remove)
        }
        enableEdgeToEdge()

        // Fix for three-button nav not properly going edge-to-edge.
        // TODO: https://issuetracker.google.com/issues/298296168
        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)
        FileKit.init(this)

        setContent {
            App(
                appState = appState,
                modifier = Modifier.fillMaxSize(),
            )
        }

        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        intent.data
            ?.let { uri ->
                val path = uri.path
                uri.query?.let { "$path?$it" } ?: path
            }
            ?.let(::GenericUri)
            ?.let(appState::onDeepLink)
            ?.also {
                intent.data = null
            }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
