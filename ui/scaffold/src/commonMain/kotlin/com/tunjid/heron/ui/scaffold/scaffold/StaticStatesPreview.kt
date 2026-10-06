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

package com.tunjid.heron.ui.scaffold.scaffold

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.media.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.identity.IdentityAction
import com.tunjid.heron.ui.scaffold.identity.IdentityState
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.NavigationState
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationState
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.ui.UiAction
import com.tunjid.heron.ui.scaffold.ui.UiState
import com.tunjid.heron.ui.scaffold.ui.UiStateHolder
import com.tunjid.heron.ui.stateproduction.StateHolderInitializer
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route

/**
 * Creates [AppScaffoldState.StaticStates] backed by no-op state holders for
 * rendering a single [route] in previews and screenshot tests.
 */
fun AppScaffoldState.StaticStates.Companion.stubForPreview(
    route: Route,
    signedInProfile: Profile?,
    imageLoader: ImageLoader,
    videoPlayerController: VideoPlayerController,
    stateHolderInitializer: StateHolderInitializer,
): AppScaffoldState.StaticStates = AppScaffoldState.StaticStates(
    identityStateHolder = stubIdentityStateHolder(
        signedInProfile = signedInProfile,
    ),
    navigationStateHolder = stubNavigationStateHolder(
        route = route,
    ),
    notificationStateHolder = stubNotificationStateHolder(),
    uiStateHolder = stubUiStateHolder(),
    imageLoader = imageLoader,
    videoPlayerController = videoPlayerController,
    stateHolderInitializer = stateHolderInitializer,
)

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

private fun stubUiStateHolder(): UiStateHolder = object :
    UiStateHolder,
    ActionSuspendingStateMutator<UiAction, UiState> by UiState.Immutable()
        .asNoOpActionSuspendingStateMutator() {}
