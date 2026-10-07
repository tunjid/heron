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

package com.tunjid.heron.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.media.images.StubImageLoader
import com.tunjid.heron.media.video.StubVideoPlayerController
import com.tunjid.heron.sheets.preview.stubSheetStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffold
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.stubForPreview
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.heron.ui.stateproduction.StateHolderInitializer
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import kotlin.reflect.KClass

@Composable
fun RoutePreview(
    route: Route,
    routeStateHolder: RouteStateHolder,
    signedInProfile: Profile? = null,
    additionalSheetStateHolderFactory: ((KClass<*>) -> SheetStateHolder?)? = null,
    render: @Composable (Route, PaneScaffoldState) -> Unit,
) {
    val stubDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher =
                NavigationEventDispatcher()
        }
    }
    val staticStates = remember(
        route,
        routeStateHolder,
        signedInProfile,
    ) {
        AppScaffoldState.StaticStates.stubForPreview(
            route = route,
            signedInProfile = signedInProfile,
            imageLoader = StubImageLoader,
            videoPlayerController = StubVideoPlayerController,
            stateHolderInitializer = object : StateHolderInitializer {
                override fun createRouteStateHolder(
                    type: KClass<out RouteStateHolder>,
                    route: Route,
                ): RouteStateHolder = routeStateHolder

                override fun createSheetStateHolder(
                    type: KClass<out SheetStateHolder>,
                ): SheetStateHolder = stubSheetStateHolder(
                    type = type,
                    additionalSheetStateHolderFactory = additionalSheetStateHolderFactory,
                )
            },
        )
    }

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides stubDispatcherOwner,
    ) {
        AppScaffold(
            modifier = Modifier,
            staticStates = staticStates,
            entryProvider = {
                threePaneEntry(
                    render = {
                        render(
                            route,
                            rememberPaneScaffoldState(),
                        )
                    },
                )
            },
        )
    }
    DisposableEffect(stubDispatcherOwner) {
        onDispose {
            stubDispatcherOwner.navigationEventDispatcher.dispose()
        }
    }
}
