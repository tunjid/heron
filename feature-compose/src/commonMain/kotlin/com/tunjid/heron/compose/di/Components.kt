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

package com.tunjid.heron.compose.di

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.ActualComposeViewModel
import com.tunjid.heron.compose.ComposeScreen
import com.tunjid.heron.compose.ComposeViewModelCreator
import com.tunjid.heron.compose.ui.ComposePostBottomBar
import com.tunjid.heron.compose.ui.links
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.Fab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_compose.generated.resources.Res
import heron.feature_compose.generated.resources.back
import heron.feature_compose.generated.resources.post
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/compose"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.creationType
    get(): Post.Create? = routeParams.queryParams["type"]?.firstOrNull()?.fromBase64EncodedUrl()

internal val Route.sharedElementPrefix
    get() = routeParams.queryParams["sharedElementPrefix"]?.firstOrNull()

@KmpComponentCreate
expect fun ComposeNavigationComponent.Companion.create(): ComposeNavigationComponent

@KmpComponentCreate
expect fun ComposeComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): ComposeComponent

@Component
abstract class ComposeNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class ComposeComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ComposeViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualComposeViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val sharedElementScope = requirePanedSharedElementScope()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    TopBar(
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                floatingActionButton = {
                    Fab(
                        modifier = Modifier
                            .alpha(if (state.postText.text.isNotBlank()) 1f else 0.6f),
                        panedSharedElementScope = sharedElementScope,
                        expanded = state.fabExpanded,
                        text = stringResource(Res.string.post),
                        icon = Icons.AutoMirrored.Rounded.Send,
                        onClick = onClick@{
                            val authorId = state.signedInProfile?.did ?: return@onClick
                            val postText = state.postText
                            viewModel.accept(
                                Action.CreatePost(
                                    postType = state.postType,
                                    authorId = authorId,
                                    text = postText.text,
                                    links = postText.annotatedString.links(),
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    val borderColor = MaterialTheme.colorScheme.outline
                    val imePadding = WindowInsets.ime.asPaddingValues()
                    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                    val imeShowing by remember {
                        derivedStateOf {
                            imePadding.calculateBottomPadding() > navBarPadding.calculateBottomPadding()
                        }
                    }
                    val hasBlankText by remember {
                        derivedStateOf { state.postText.text.isBlank() }
                    }
                    ComposePostBottomBar(
                        modifier = Modifier
                            .drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1f,
                                )
                            }
                            .padding(horizontal = 8.dp)
                            .padding(
                                imePadding.takeIf {
                                    imeShowing
                                } ?: navBarPadding
                            ),
                        postText = state.postText,
                    )

                    DisposableEffect(hasBlankText, imeShowing) {
                        val fabExpanded = hasBlankText || !imeShowing
                        viewModel.accept(Action.SetFabExpanded(expanded = fabExpanded))
                        onDispose { }
                    }
                },
                content = { paddingValues ->
                    ComposeScreen(
                        panedSharedElementScope = requirePanedSharedElementScope(),
                        modifier = Modifier
                            .padding(paddingValues = paddingValues),
                        state = state,
                        actions = viewModel.accept,
                    )
                }
            )
        }
    )
}

@Composable
private fun TopBar(
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            FilledTonalIconButton(
                modifier = Modifier,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.9f
                    )
                ),
                onClick = onBackPressed,
            ) {
                Image(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                )
            }
        },
        title = {},
    )
}
