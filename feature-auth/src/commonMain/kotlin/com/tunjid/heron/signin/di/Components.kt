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

package com.tunjid.heron.signin.di

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.signin.Action
import com.tunjid.heron.signin.ActualSignInViewModel
import com.tunjid.heron.signin.RouteViewModelInitializer
import com.tunjid.heron.signin.SignInScreen
import com.tunjid.heron.signin.sessionRequest
import com.tunjid.heron.signin.submitButtonEnabled
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Extends
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature_auth.generated.resources.Res
import heron.feature_auth.generated.resources.create_an_account
import heron.feature_auth.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/auth"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@DependencyGraph(isExtendable = true)
interface SignInNavigationComponent {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute
        )
}

@DependencyGraph(isExtendable = true)
interface SignInComponent {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Extends dataComponent: DataComponent,
            @Extends scaffoldComponent: ScaffoldComponent,
        ): SignInComponent
    }

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    @OptIn(ExperimentalSharedTransitionApi::class)
    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        render = { route ->
            val viewModel = viewModel<ActualSignInViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this),
                showNavigation = false,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.MessageConsumed(it))
                },
                topBar = {
                    TopBar()
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .animateBounds(lookaheadScope = this)
                            .windowInsetsPadding(WindowInsets.ime)
                            .alpha(if (state.submitButtonEnabled) 1f else 0.6f),
                        text = stringResource(Res.string.sign_in),
                        icon = Icons.Rounded.Check,
                        expanded = true,
                        onClick = {
                            if (state.submitButtonEnabled) viewModel.accept(Action.Submit(state.sessionRequest))
                        }
                    )
                },
                content = { paddingValues ->
                    SignInScreen(
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(paddingValues = paddingValues),
                    )
                }
            )
        }
    )
}

@Composable
private fun TopBar() {
    TopAppBar(
        title = {},
        actions = {
            TextButton(
                onClick = {},
                content = {
                    Text(
                        text = stringResource(Res.string.create_an_account),
                        maxLines = 1,
                    )
                }
            )
        },
    )
}
