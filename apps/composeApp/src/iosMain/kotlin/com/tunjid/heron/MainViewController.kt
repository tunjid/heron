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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import com.tunjid.heron.ui.scaffold.scaffold.AppState.Companion.isImmersive

@Suppress("FunctionName")
fun MainViewController(
    appState: AppState,
    immersionController: ImmersionController,
) = ComposeUIViewController {
    com.tunjid.heron.ui.scaffold.scaffold.App(
        appState = appState,
        modifier = Modifier.fillMaxSize(),
    )
    val immersiveState = rememberUpdatedState(appState.isImmersive)

    DisposableEffect(immersionController) {
        onDispose {
            immersionController.setImmersive(isImmersive = false)
        }
    }
    LaunchedEffect(immersionController) {
        snapshotFlow(immersiveState::value)
            .collect { isImmersive ->
                immersionController.setImmersive(isImmersive = isImmersive)
            }
    }
}
