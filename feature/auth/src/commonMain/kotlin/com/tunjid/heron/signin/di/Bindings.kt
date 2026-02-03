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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.scaffold.scaffold.AppLogo
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.signin.Action
import com.tunjid.heron.signin.ActualSignInViewModel
import com.tunjid.heron.signin.AuthMode
import com.tunjid.heron.signin.RouteViewModelInitializer
import com.tunjid.heron.signin.SignInScreen
import com.tunjid.heron.signin.authMode
import com.tunjid.heron.signin.canSignInLater
import com.tunjid.heron.signin.createSessionAction
import com.tunjid.heron.signin.submitButtonEnabled
import com.tunjid.heron.signin.ui.stringResource
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.auth.generated.resources.Res
import heron.feature.auth.generated.resources.sign_in
import heron.feature.auth.generated.resources.sign_in_later
import heron.feature.auth.generated.resources.sign_with_oauth
import heron.feature.auth.generated.resources.sign_with_password
import heron.feature.auth.generated.resources.signing_in
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/auth"
private const val OAuthPattern = "/oauth/callback"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

// The issuer endpoint for an oauth token
internal val Route.iss by optionalRouteQuery()

@BindingContainer
object SignInNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(OAuthPattern)
    fun provideOAuthRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = OAuthPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class SignInBindings(
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

    @Provides
    @IntoMap
    @StringKey(OAuthPattern)
    fun provideOAuthPaneEntry(
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
            val paneScaffoldState = rememberPaneScaffoldState()

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState),
                showNavigation = false,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.MessageConsumed(it))
                },
                topBar = {
                    TopBar(
                        authMode = state.authMode,
                        oauthAvailable = state.isOauthAvailable,
                        selectedServer = state.selectedServer,
                        onPasswordPreferenceToggled = {
                            viewModel.accept(Action.TogglePasswordPreference)
                        },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .animateBounds(lookaheadScope = this)
                            .windowInsetsPadding(WindowInsets.ime),
                        text = stringResource(
                            when {
                                state.isSubmitting -> Res.string.signing_in
                                state.canSignInLater -> Res.string.sign_in_later
                                else -> when (state.authMode) {
                                    AuthMode.Oauth -> Res.string.sign_with_oauth
                                    AuthMode.Password -> Res.string.sign_with_password
                                }
                            },
                            stringResource(state.selectedServer.stringResource),
                        ),
                        icon = when {
                            state.canSignInLater -> Icons.Rounded.Timer
                            else -> Icons.Rounded.Check
                        },
                        enabled = state.submitButtonEnabled,
                        expanded = true,
                        onClick = {
                            viewModel.accept(state.createSessionAction())
                        },
                    )
                },
                content = { paddingValues ->
                    SignInScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(paddingValues = paddingValues),
                    )
                },
            )
        },
    )
}

@Composable
private fun PaneScaffoldState.TopBar(
    authMode: AuthMode,
    oauthAvailable: Boolean,
    selectedServer: Server,
    onPasswordPreferenceToggled: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            AppLogo(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(36.dp),
                isRootDestination = true,
            )
        },
        title = {
            AppBarTitle(
                title = stringResource(Res.string.sign_in),
            )
        },
        actions = {
            if (oauthAvailable) Box(
                modifier = Modifier
                    .padding(8.dp),
            ) {
                var expanded by remember {
                    mutableStateOf(false)
                }
                IconButton(
                    onClick = {
                        expanded = !expanded
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                        )
                    },
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    },
                    content = {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        when (authMode) {
                                            AuthMode.Oauth -> Res.string.sign_with_password
                                            AuthMode.Password -> Res.string.sign_with_oauth
                                        },
                                        stringResource(selectedServer.stringResource),
                                    ),
                                )
                            },
                            onClick = {
                                onPasswordPreferenceToggled()
                                expanded = false
                            },
                        )
                    },
                )
            }
        },
    )
}
