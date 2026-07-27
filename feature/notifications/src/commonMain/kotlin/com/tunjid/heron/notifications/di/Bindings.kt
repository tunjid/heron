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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.notifications.Action
import com.tunjid.heron.notifications.ActualNotificationsViewModel
import com.tunjid.heron.notifications.NotificationsScreen
import com.tunjid.heron.notifications.NotificationsStateHolder
import com.tunjid.heron.notifications.NotificationsViewModelInitializer
import com.tunjid.heron.notifications.ui.RequestNotificationsButton
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.NavigationScope
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.notificationSettingsDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.notifications.hasNotificationPermissions
import com.tunjid.heron.ui.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.notifications.generated.resources.Res
import heron.feature.notifications.generated.resources.title
import heron.ui.core.generated.resources.notification_settings
import heron.ui.core.generated.resources.notifications_create_post
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/notifications"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
@ContributesTo(NavigationScope::class)
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
@ContributesTo(AppScope::class)
object NotificationsBindings {

    @Provides
    @IntoMap
    @ClassKey(NotificationsStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: NotificationsViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry(
        contentTransform = navigationContentTransformer::contentTransform,
        render = { route ->
            Route(
                route = route,
                paneScaffoldState = rememberPaneScaffoldState(),
            )
        },
    )
}

@Composable
internal fun Route(
    route: Route,
    paneScaffoldState: PaneScaffoldState,
) {
    val stateHolder = paneScaffoldState.retainRouteStateHolder<NotificationsStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val topAppBarNestedScrollConnection =
        paneScaffoldState.topAppBarNestedScrollConnection

    val bottomNavigationNestedScrollConnection =
        paneScaffoldState.bottomNavigationNestedScrollConnection

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
            .nestedScroll(topAppBarNestedScrollConnection)
            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                nestedScroll(bottomNavigationNestedScrollConnection)
            },
        showNavigation = true,
        snackBarMessages = state.messages,
        onSnackBarMessageConsumed = {
            stateHolder.accept(Action.SnackbarDismissed(it))
        },
        topBar = {
            RootDestinationTopAppBar(
                modifier = Modifier.offset {
                    topAppBarNestedScrollConnection.offset.round()
                },
                title = {
                    AppBarTitle(
                        title = stringResource(Res.string.title),
                    )
                },
                transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                onSignedInProfileClicked = { profile, sharedElementKey ->
                    stateHolder.accept(
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !hasNotificationPermissions(),
                        ) {
                            RequestNotificationsButton(
                                animateIcon = state.canAnimateRequestPermissionsButton,
                            )
                        }
                        AppBarIconButton(
                            icon = Icons.Rounded.Settings,
                            iconDescription = stringResource(CommonStrings.notification_settings),
                            onClick = {
                                stateHolder.accept(
                                    Action.Navigate.To(notificationSettingsDestination()),
                                )
                            },
                        )
                    }
                },
                onLogoClicked = {
                    stateHolder.accept(Action.Navigate.Home)
                },
            )
        },
        floatingActionButton = {
            PaneFab(
                modifier = Modifier
                    .offset {
                        fabOffset(bottomNavigationNestedScrollConnection.offset)
                    },
                text = stringResource(CommonStrings.notifications_create_post),
                icon = Icons.Rounded.Edit,
                expanded = isFabExpanded {
                    if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset
                    else topAppBarNestedScrollConnection.offset * -1f
                },
                onClick = {
                    stateHolder.accept(
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
                        bottomNavigationNestedScrollConnection.offset.round()
                    },
                onNavItemReselected = {
                    stateHolder.accept(Action.Tile(TilingState.Action.Refresh))
                    true
                },
            )
        },
        navigationRail = {
            PaneNavigationRail(
                onNavItemReselected = {
                    stateHolder.accept(Action.Tile(TilingState.Action.Refresh))
                    true
                },
            )
        },
        content = {
            NotificationsScreen(
                paneScaffoldState = this,
                state = state,
                actions = stateHolder.accept,
            )
        },
    )
}
