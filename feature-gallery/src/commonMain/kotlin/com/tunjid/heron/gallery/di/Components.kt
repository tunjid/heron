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

package com.tunjid.heron.gallery.di

import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.gallery.ActualGalleryViewModel
import com.tunjid.heron.gallery.GalleryScreen
import com.tunjid.heron.gallery.GalleryViewModelCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routePatternAndMatcher
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.dragToPop
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.mappedRouteQuery
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routeQuery
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/post/{postId}/gallery"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.media: Embed.Media? by optionalMappedRouteQuery(
    mapper = String::fromBase64EncodedUrl
)

internal val Route.postId by mappedRoutePath(
    mapper = ::Id,
)

internal val Route.startIndex by mappedRouteQuery(
    default = 0,
    mapper = String::toInt,
)

internal val Route.sharedElementPrefix by routeQuery(
    default = ""
)

@KmpComponentCreate
expect fun GalleryNavigationComponent.Companion.create(): GalleryNavigationComponent

@KmpComponentCreate
expect fun GalleryComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): GalleryComponent

@Component
abstract class GalleryNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class GalleryComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: GalleryViewModelCreator,
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualGalleryViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .dragToPop(),
                showNavigation = false,
                containerColor = Color.Transparent,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                content = {
                    GalleryScreen(
                        paneScaffoldState = this,
                        modifier = Modifier,
                        state = state,
                    )
                }
            )
        }
    )
}

