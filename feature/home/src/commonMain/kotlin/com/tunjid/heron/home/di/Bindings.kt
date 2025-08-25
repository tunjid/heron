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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.home.Action
import com.tunjid.heron.home.ActualHomeViewModel
import com.tunjid.heron.home.HomeScreen
import com.tunjid.heron.home.RouteViewModelInitializer
import com.tunjid.heron.home.timelinePreferenceExpansionEffect
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.ScaffoldStrings
import com.tunjid.heron.scaffold.scaffold.fabOffset
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.UiTokens
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
import heron.feature.home.generated.resources.Res
import heron.feature.home.generated.resources.create_post
import heron.feature.home.generated.resources.save
import heron.scaffold.generated.resources.sign_in
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
    fun provideRouteMatcher(): RouteMatcher = urlRouteMatcher(
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
            val viewModel = viewModel<ActualHomeViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val statusBarHeight = UiTokens.statusBarHeight
            val topAppBarOffsetNestedScrollConnection =
                rememberAccumulatedOffsetNestedScrollConnection(
                    maxOffset = { Offset.Zero },
                    minOffset = {
                        Offset(
                            x = 0f,
                            y = -(statusBarHeight + UiTokens.toolbarHeight).toPx(),
                        )
                    },
                )
            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .nestedScroll(topAppBarOffsetNestedScrollConnection)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarOffsetNestedScrollConnection.offset.round()
                        },
                        signedInProfile = state.signedInProfile,
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
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .height(statusBarHeight)
                            .fillMaxWidth(),
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
                                isSignedOut -> ScaffoldStrings.sign_in
                                state.timelinePreferencesExpanded -> Res.string.save
                                else -> Res.string.create_post
                            },
                        ),
                        icon = when {
                            isSignedOut -> Icons.AutoMirrored.Rounded.Login
                            state.timelinePreferencesExpanded -> Icons.Rounded.Save
                            else -> Icons.Rounded.Edit
                        },
                        expanded = isFabExpanded(bottomNavigationNestedScrollConnection.offset),
                        onClick = {
                            viewModel.accept(
                                when {
                                    isSignedOut -> Action.Navigate.To(
                                        signInDestination(),
                                    )

                                    state.timelinePreferencesExpanded -> Action.UpdateTimeline.RequestUpdate
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
                            viewModel.accept(Action.RefreshCurrentTab)
                            true
                        },
                    )
                },
                navigationRail = {
                    PaneNavigationRail(
                        onNavItemReselected = {
                            viewModel.accept(Action.RefreshCurrentTab)
                            true
                        },
                    )
                },
                content = { contentPadding ->
                    HomeScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(top = contentPadding.calculateTopPadding()),
                    )
                },
            )

            topAppBarOffsetNestedScrollConnection.timelinePreferenceExpansionEffect(
                isExpanded = state.timelinePreferencesExpanded,
            )
        },
    )
}
