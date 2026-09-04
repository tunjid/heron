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

package com.tunjid.heron.di

import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.media.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindingArgs
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import com.tunjid.heron.ui.scaffold.ui.UiStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(
    scope = AppScope::class,
)
interface AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides dataBindingArgs: DataBindingArgs,
            @Provides scaffoldBindingArgs: ScaffoldBindingArgs,
        ): AppGraph
    }

    val entryMap: Map<String, PaneEntry<ThreePane, Route>>

    @SingleIn(AppScope::class)
    @Provides
    fun appState(
        @AppMainScope
        appMainScope: CoroutineScope,
        identityStateHolder: IdentityStateHolder,
        navigationStateHolder: NavigationStateHolder,
        notificationStateHolder: NotificationStateHolder,
        uiStateHolder: UiStateHolder,
        imageLoader: ImageLoader,
        videoPlayerController: VideoPlayerController,
        sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
        routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
        backgroundTaskScheduler: BackgroundTaskScheduler,
    ): AppState = AppState(
        entryMap = entryMap,
        identityStateHolder = identityStateHolder,
        navigationStateHolder = navigationStateHolder,
        notificationStateHolder = notificationStateHolder,
        uiStateHolder = uiStateHolder,
        imageLoader = imageLoader,
        videoPlayerController = videoPlayerController,
        sheetStateHolderInitializers = sheetStateHolderInitializers,
        routeStateHolderInitializers = routeStateHolderInitializers,
        backgroundTaskScheduler = backgroundTaskScheduler,
    )

    val appState: AppState
}
