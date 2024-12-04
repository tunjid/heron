/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.signin.di

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.signin.Action
import com.tunjid.heron.signin.ActualSignInStateHolder
import com.tunjid.heron.signin.SignInScreen
import com.tunjid.heron.signin.SignInStateHolderCreator
import com.tunjid.heron.signin.sessionRequest
import com.tunjid.heron.signin.submitButtonEnabled
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_auth.generated.resources.Res
import heron.feature_auth.generated.resources.create_an_account
import heron.feature_auth.generated.resources.sign_in
import heron.feature_auth.generated.resources.submit
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/auth"

private fun signInRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@Component
abstract class SignInNavigationComponent {

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::signInRoute,
        )

}

@Component
abstract class SignInScreenHolderComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SignInStateHolderCreator
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSignInStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = false,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.MessageConsumed(it))
                },
                topBar = {
                    TopBar()
                },
                floatingActionButton = {
                    Fab(
                        modifier = Modifier.alpha(if (state.submitButtonEnabled) 1f else 0.6f),
                        onSubmit = {
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

@Composable
private fun Fab(
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = onSubmit,
        content = {
            Text(stringResource(Res.string.sign_in))
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(Res.string.submit)
            )
        }
    )
}
