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

package com.tunjid.heron.notifications.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.notifications.Action
import com.tunjid.heron.notifications.ActualNotificationsViewModel
import com.tunjid.heron.notifications.NotificationsScreen
import com.tunjid.heron.notifications.RouteViewModelInitializer
import com.tunjid.heron.notifications.ui.RequestNotificationsButton
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.notifications.hasNotificationPermissions
import com.tunjid.heron.scaffold.scaffold.LocalAppState
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.fabOffset
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.navigationBarOffset
import com.tunjid.heron.ui.fabPositionOffset
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.ui.core.generated.resources.notifications_create_post
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/notifications"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object NotificationsNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class NotificationsBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        render = { route ->
            val viewModel = viewModel<ActualNotificationsViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            val appState = LocalAppState.current

            val autoHideNavigationBar = appState.preferences?.autoHideNavigationBar ?: true

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                    enabled = autoHideNavigationBar,
                )

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .nestedScroll(topAppBarNestedScrollConnection)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarNestedScrollConnection.offset.round()
                        },
                        signedInProfile = state.signedInProfile,
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onSignedInProfileClicked = { profile, sharedElementKey ->
                            viewModel.accept(
                                Action.Navigate.To(
                                    profileDestination(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = profile,
                                        avatarSharedElementKey = sharedElementKey,
                                    ),
                                ),
                            )
                        },
                        actions = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !hasNotificationPermissions(),
                            ) {
                                RequestNotificationsButton(
                                    animateIcon = state.canAnimateRequestPermissionsButton,
                                )
                            }
                        },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.fabPositionOffset(autoHideNavigationBar))
                            },
                        text = stringResource(CommonStrings.notifications_create_post),
                        icon = Icons.Rounded.Edit,
                        expanded = isFabExpanded(bottomNavigationNestedScrollConnection.offset),
                        onClick = {
                            viewModel.accept(
                                Action.Navigate.To(
                                    composePostDestination(
                                        type = Post.Create.Timeline,
                                        sharedElementPrefix = null,
                                    ),
                                ),
                            )
                        },
                    )
                },
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.navigationBarOffset(autoHideNavigationBar).round()
                            },
                        onNavItemReselected = {
                            viewModel.accept(Action.Tile(TilingState.Action.Refresh))
                            true
                        },
                    )
                },
                navigationRail = {
                    PaneNavigationRail(
                        onNavItemReselected = {
                            viewModel.accept(Action.Tile(TilingState.Action.Refresh))
                            true
                        },
                    )
                },
                content = {
                    NotificationsScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                    )
                },
            )
        },
    )
}
