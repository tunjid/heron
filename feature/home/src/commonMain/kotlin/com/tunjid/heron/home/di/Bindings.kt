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

package com.tunjid.heron.home.di

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.home.Action
import com.tunjid.heron.home.ActualHomeViewModel
import com.tunjid.heron.home.HomeScreen
import com.tunjid.heron.home.HomeStateHolder
import com.tunjid.heron.home.HomeViewModelInitializer
import com.tunjid.heron.home.TabLayout
import com.tunjid.heron.home.ui.TabsExpansionEffect
import com.tunjid.heron.home.ui.TrendsTicker
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.ui.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberRouteViewModel
import com.tunjid.heron.ui.stateproduction.RouteViewModelInitializer
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
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
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.home.generated.resources.Res
import heron.feature.home.generated.resources.create_post
import heron.feature.home.generated.resources.save
import heron.ui.core.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource

internal const val RoutePattern = "/home"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object HomeNavigationBindings {

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
class HomeBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @ClassKey(ActualHomeViewModel::class)
    fun provideRouteViewModelInitializer(
        initializer: HomeViewModelInitializer,
    ): RouteViewModelInitializer = RouteViewModelInitializer(initializer::invoke)

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
            val paneScaffoldState = rememberPaneScaffoldState()
            val stateHolder: HomeStateHolder = paneScaffoldState.rememberRouteViewModel<ActualHomeViewModel>(
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
                    RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarNestedScrollConnection.offset.round()
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        title = {
                            AnimatedVisibility(
                                visible = state.preferences.local.showTrendingTopics,
                                enter = remember(::fadeIn),
                                exit = remember(::fadeOut),
                            ) {
                                TrendsTicker(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    sharedTransitionScope = paneScaffoldState,
                                    trends = state.trends,
                                    onTrendClicked = { trend ->
                                        stateHolder.accept(
                                            Action.Navigate.To(
                                                pathDestination(
                                                    path = trend.link,
                                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                                ),
                                            ),
                                        )
                                    },
                                )
                            }
                        },
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
                        onLogoClicked = {
                            stateHolder.accept(
                                Action.SetTabLayout(
                                    layout = if (state.tabLayout is TabLayout.Expanded) {
                                        TabLayout.Collapsed.All
                                    } else {
                                        TabLayout.Expanded
                                    },
                                ),
                            )
                        },
                    )
                },
                snackBarHost = {
                    PaneSnackbarHost(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.offset)
                            },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.offset)
                            },
                        text = stringResource(
                            when {
                                isSignedOut -> CommonStrings.sign_in
                                state.tabLayout is TabLayout.Expanded -> Res.string.save
                                else -> Res.string.create_post
                            },
                        ),
                        icon = when {
                            isSignedOut -> Icons.AutoMirrored.Rounded.Login
                            state.tabLayout is TabLayout.Expanded -> Icons.Rounded.Save
                            else -> Icons.Rounded.Edit
                        },
                        expanded = isFabExpanded {
                            if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset
                            else topAppBarNestedScrollConnection.offset * -1f
                        },
                        onClick = {
                            stateHolder.accept(
                                when {
                                    isSignedOut -> Action.Navigate.To(
                                        signInDestination(),
                                    )

                                    state.tabLayout is TabLayout.Expanded -> Action.UpdateTimeline.RequestUpdate
                                    else -> Action.Navigate.To(
                                        composePostDestination(
                                            type = Post.Create.Timeline,
                                            sharedElementPrefix = null,
                                        ),
                                    )
                                },
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
                            stateHolder.accept(Action.RefreshCurrentTab)
                            true
                        },
                    )
                },
                navigationRail = {
                    PaneNavigationRail(
                        onNavItemReselected = {
                            stateHolder.accept(Action.RefreshCurrentTab)
                            true
                        },
                    )
                },
                content = {
                    HomeScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = stateHolder.accept,
                    )
                },
            )

            topAppBarNestedScrollConnection.TabsExpansionEffect(
                isExpanded = state.tabLayout is TabLayout.Expanded,
            )
        },
    )
}
