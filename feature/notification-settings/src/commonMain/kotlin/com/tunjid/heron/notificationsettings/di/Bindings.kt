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

package com.tunjid.heron.notificationsettings.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.notificationsettings.Action
import com.tunjid.heron.notificationsettings.ActualNotificationSettingsViewModel
import com.tunjid.heron.notificationsettings.NotificationSettingsScreen
import com.tunjid.heron.notificationsettings.NotificationSettingsStateHolder
import com.tunjid.heron.notificationsettings.NotificationSettingsViewModelInitializer
import com.tunjid.heron.notificationsettings.updates
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.ui.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberRouteViewModel
import com.tunjid.heron.ui.stateproduction.RouteViewModelInitializer
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.ui.core.generated.resources.notification_settings
import heron.ui.core.generated.resources.save
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/settings/notifications"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

@BindingContainer
object NotificationSettingsNavigationBindings {

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
class NotificationSettingsBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @ClassKey(ActualNotificationSettingsViewModel::class)
    fun provideRouteViewModelInitializer(
        initializer: NotificationSettingsViewModelInitializer,
    ): RouteViewModelInitializer = RouteViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry<Route>(
        contentTransform = navigationContentTransformer::contentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            Route(
                route = routeParser.hydrate(route),
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
    val stateHolder: NotificationSettingsStateHolder = paneScaffoldState.rememberRouteViewModel<ActualNotificationSettingsViewModel>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val topAppBarNestedScrollConnection =
        topAppBarNestedScrollConnection()

    val bottomNavigationNestedScrollConnection =
        bottomNavigationNestedScrollConnection(
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        )

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
            PoppableDestinationTopAppBar(
                title = {
                    AppBarTitle(title = stringResource(CommonStrings.notification_settings))
                },
                onBackPressed = { stateHolder.accept(Action.Navigate.Pop) },
            )
        },
        floatingActionButton = {
            PaneFab(
                modifier = Modifier
                    .offset {
                        fabOffset(bottomNavigationNestedScrollConnection.offset)
                    },
                text = stringResource(CommonStrings.save),
                icon = Icons.Rounded.Save,
                enabled = state.pendingUpdates.isNotEmpty(),
                expanded = isFabExpanded {
                    if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset
                    else topAppBarNestedScrollConnection.offset * -1f
                },
                onClick = {
                    stateHolder.accept(
                        Action.UpdateNotificationPreferences(state.updates()),
                    )
                },
            )
        },
        navigationBar = {
            PaneNavigationBar(
                modifier = Modifier.offset {
                    bottomNavigationNestedScrollConnection.offset.round()
                },
            )
        },
        navigationRail = {
            PaneNavigationRail()
        },
        content = { paddingValues ->
            NotificationSettingsScreen(
                paneScaffoldState = this,
                state = state,
                actions = stateHolder.accept,
                modifier = Modifier
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                    ),
            )
            SecondaryPaneCloseBackHandler()
        },
    )
}
