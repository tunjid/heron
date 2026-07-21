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

import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.data.logging.Logger
import com.tunjid.heron.data.platform.Platform
import com.tunjid.heron.data.platform.current
import com.tunjid.heron.di.AppGraph
import com.tunjid.heron.di.AppNavigationGraph
import com.tunjid.heron.di.allRouteMatchers
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindingArgs
import com.tunjid.heron.ui.scaffold.notifications.Notifier
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createAppState(
    imageLoader: () -> ImageLoader,
    notifier: (appMainScope: CoroutineScope) -> Notifier,
    logger: () -> Logger,
    videoPlayerController: (appMainScope: CoroutineScope) -> VideoPlayerController,
    args: (appMainScope: CoroutineScope) -> DataBindingArgs,
): AppState {
    with(Platform.current) {
        if (supportsComposeDiagnosticStackTraces) Composer.setDiagnosticStackTraceMode(
            if (isRelease) ComposeStackTraceMode.Auto
            else ComposeStackTraceMode.SourceInformation,
        )
    }

    logger().install()

    val appMainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val navigationComponent = createGraphFactory<AppNavigationGraph.Factory>().create()

    val appGraph = createGraphFactory<AppGraph.Factory>().create(
        dataBindingArgs = args(appMainScope),
        scaffoldBindingArgs = ScaffoldBindingArgs(
            imageLoader = imageLoader(),
            notifier = notifier(appMainScope),
            videoPlayerController = videoPlayerController(appMainScope),
            routeMatchers = navigationComponent.allRouteMatchers,
        ),
    )
    return appGraph.appState
}
