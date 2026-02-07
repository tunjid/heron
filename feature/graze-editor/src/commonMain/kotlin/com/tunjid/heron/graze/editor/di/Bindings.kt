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

package com.tunjid.heron.graze.editor.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.graze.editor.Action
import com.tunjid.heron.graze.editor.ActualGrazeEditorViewModel
import com.tunjid.heron.graze.editor.FilterNavigationEventInfo
import com.tunjid.heron.graze.editor.GrazeEditorScreen
import com.tunjid.heron.graze.editor.RouteViewModelInitializer
import com.tunjid.heron.graze.editor.currentFilter
import com.tunjid.heron.graze.editor.ui.AddFilterDialog
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransformProvider
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.text.CommonStrings
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
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.graze_editor
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/graze_editor"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

@BindingContainer
object GrazeEditorNavigationBindings {

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
class GrazeEditorBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = threePaneEntry(
        contentTransform = predictiveBackContentTransformProvider(),
        render = { route ->
            val viewModel = viewModel<ActualGrazeEditorViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            var isAdding by rememberSaveable {
                mutableStateOf(false)
            }

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = { Text(stringResource(Res.string.graze_editor)) },
                        onBackPressed = {
                            if (state.currentPath.isNotEmpty()) viewModel.accept(Action.ExitFilter)
                            else viewModel.accept(Action.Navigate.Pop)
                        },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        text = "Add filter",
                        icon = Icons.Rounded.Add,
                        expanded = true,
                        onClick = {
                            isAdding = true
                        },
                    )
                },
                content = {
                    GrazeEditorScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                    )
                },
            )

            if (isAdding) AddFilterDialog(
                onDismissRequest = {
                    isAdding = false
                },
                onFilterSelected = {
                    isAdding = false
                    viewModel.accept(Action.AddFilter(it))
                },
            )

            NavigationBackHandler(
                state = rememberNavigationEventState(
                    currentInfo = FilterNavigationEventInfo(state.currentFilter),
                ),
                isBackEnabled = state.currentPath.isNotEmpty(),
            ) {
                viewModel.accept(Action.ExitFilter)
            }
        },
    )
}
