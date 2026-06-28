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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.StubImageLoader
import com.tunjid.heron.media.video.StubVideoPlayerController
import com.tunjid.heron.sheets.preview.stubSheetViewModelInitializer
import com.tunjid.heron.ui.scaffold.identity.IdentityAction
import com.tunjid.heron.ui.scaffold.identity.IdentityState
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.NavigationState
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationState
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffold
import com.tunjid.heron.ui.scaffold.scaffold.DisplayScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.stateproduction.RouteViewModel
import com.tunjid.heron.ui.stateproduction.RouteViewModelInitializer
import com.tunjid.heron.ui.stateproduction.SheetViewModel
import com.tunjid.heron.ui.stateproduction.SheetViewModelInitializer
import com.tunjid.heron.ui.stateproduction.ViewModelInitializer
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import kotlin.reflect.KClass

@Composable
fun RoutePreview(
    routeViewModel: RouteViewModel,
    signedInProfile: Profile? = null,
    render: @Composable (Route, PaneScaffoldState) -> Unit,
) {
    val staticStates = remember(
        routeViewModel,
        signedInProfile,
    ) {
        DisplayScaffoldState.StaticStates(
            identityStateHolder = stubIdentityStateHolder(
                signedInProfile = signedInProfile,
            ),
            navigationStateHolder = stubNavigationStateHolder(
                route = routeViewModel.route,
            ),
            notificationStateHolder = stubNotificationStateHolder(),
            imageLoader = StubImageLoader,
            videoPlayerController = StubVideoPlayerController,
            viewModelInitializer = object : ViewModelInitializer {
                override fun sheetViewModelInitializer(
                    modelClass: KClass<out SheetViewModel>,
                ): SheetViewModelInitializer = stubSheetViewModelInitializer(modelClass)

                override fun routeViewModelInitializer(
                    modelClass: KClass<out RouteViewModel>,
                ): RouteViewModelInitializer = RouteViewModelInitializer { _, _ ->
                    routeViewModel
                }
            },
        )
    }

    AppScaffold(
        modifier = Modifier,
        staticStates = staticStates,
        entryProvider = {
            threePaneEntry(
                render = {
                    render(
                        routeViewModel.route,
                        rememberPaneScaffoldState(),
                    )
                },
            )
        },
    )
}

private fun stubIdentityStateHolder(
    signedInProfile: Profile?,
): IdentityStateHolder = object :
    IdentityStateHolder,
    ActionSuspendingStateMutator<IdentityAction, IdentityState> by IdentityState.Immutable(
        signedInProfile = signedInProfile,
    )
        .asNoOpActionSuspendingStateMutator() {}

private fun stubNavigationStateHolder(
    route: Route,
): NavigationStateHolder = object :
    NavigationStateHolder,
    ActionSuspendingStateMutator<NavigationMutation, NavigationState> by NavigationState.Immutable(
        multiStackNav = NavigationState.Immutable()
            .multiStackNav
            .push(route),
    )
        .asNoOpActionSuspendingStateMutator() {}

private fun stubNotificationStateHolder(): NotificationStateHolder = object :
    NotificationStateHolder,
    ActionSuspendingStateMutator<NotificationAction, NotificationState> by NotificationState.Immutable()
        .asNoOpActionSuspendingStateMutator() {}
