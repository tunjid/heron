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

package com.tunjid.heron.feed.di

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.feed.Action
import com.tunjid.heron.feed.ActualFeedStateHolder
import com.tunjid.heron.feed.FeedScreen
import com.tunjid.heron.feed.FeedStateHolderCreator
import com.tunjid.heron.feed.ui.Timeline
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_feed.generated.resources.Res
import heron.feature_feed.generated.resources.back
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/profile/{profileId}/feed/{feedId}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.feedLookup
    get() = UriLookup.Timeline.FeedGenerator(
        profileHandleOrDid = routeParams.pathArgs.getValue("profileId"),
        feedUriSuffix = routeParams.pathArgs.getValue("feedId"),
    )

@KmpComponentCreate
expect fun FeedNavigationComponent.Companion.create(): FeedNavigationComponent

@KmpComponentCreate
expect fun FeedComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): FeedComponent

@Component
abstract class FeedNavigationComponent {
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
abstract class FeedComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: FeedStateHolderCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualFeedStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    val timelineStateHolder = remember(state.timelineStateHolder) {
                        state.timelineStateHolder
                    }
                    TopBar(
                        timeline = timelineStateHolder?.state?.collectAsStateWithLifecycle()?.value?.timeline,
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) }
                    )
                },
                content = { paddingValues ->
                    FeedScreen(
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
    timeline: Timeline?,
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
        title = {
            Timeline(
                timeline = timeline,
            )
        },
    )
}
