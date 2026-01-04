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

package com.tunjid.heron.messages.di

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ForwardToInbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.round
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.messages.Action
import com.tunjid.heron.messages.ActualMessagesViewModel
import com.tunjid.heron.messages.MessagesScreen
import com.tunjid.heron.messages.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.LocalAppState
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.fabOffset
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.SearchBar
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.fabPositionOffset
import com.tunjid.heron.ui.navigationBarOffset
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
import heron.feature.messages.generated.resources.Res
import heron.feature.messages.generated.resources.write_new_dm
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/messages"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object MessagesNavigationBindings {

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
class MessagesBindings(
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
            val viewModel = viewModel<ActualMessagesViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            val appState = LocalAppState.current
            val autoHideNavigationBar = appState.preferences?.autoHideNavigationBar ?: true

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                    enabled = autoHideNavigationBar,
                )

            val searchFocusRequester = remember { FocusRequester() }

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
                        title = {
                            val keyboardController = LocalSoftwareKeyboardController.current
                            AnimatedVisibility(
                                visible = state.isSearching,
                                enter = SearchbarEnterAnimation,
                                exit = SearchbarExitAnimation,
                            ) {
                                SearchBar(
                                    searchQuery = state.searchQuery,
                                    focusRequester = searchFocusRequester,
                                    onQueryChanged = { query ->
                                        viewModel.accept(Action.SearchQueryChanged(query))
                                    },
                                    onQueryConfirmed = {
                                        viewModel.accept(Action.SearchQueryChanged(query = ""))
                                        keyboardController?.hide()
                                    },
                                )
                                LaunchedEffect(Unit) {
                                    searchFocusRequester.requestFocus()
                                }
                            }
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
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.fabPositionOffset(autoHideNavigationBar))
                            },
                        text = stringResource(Res.string.write_new_dm),
                        icon = Icons.AutoMirrored.Rounded.ForwardToInbox,
                        expanded = isFabExpanded(bottomNavigationNestedScrollConnection.offset),
                        onClick = {
                            viewModel.accept(Action.SetIsSearching(isSearching = true))
                        },
                    )
                },
                snackBarHost = {
                    PaneSnackbarHost(
                        modifier = Modifier
                            .imePadding(),
                    )
                },
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.navigationBarOffset(autoHideNavigationBar).round()
                            },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                content = {
                    MessagesScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .imePadding(),
                    )
                },
            )

            val imePadding = WindowInsets.ime.asPaddingValues()
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
            val imeShowing by remember {
                derivedStateOf {
                    imePadding.calculateBottomPadding() > navBarPadding.calculateBottomPadding()
                }
            }

            val lifeCycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(imeShowing, lifeCycleOwner) {
                if (imeShowing) return@LaunchedEffect
                if (!lifeCycleOwner.lifecycle.currentState
                        .isAtLeast(Lifecycle.State.RESUMED)
                ) return@LaunchedEffect

                searchFocusRequester.freeFocus()
                viewModel.accept(Action.SetIsSearching(isSearching = false))
            }
        },
    )
}

private val SearchbarEnterAnimation = fadeIn() + slideInVertically()
private val SearchbarExitAnimation = fadeOut() + slideOutVertically()
