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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.graze.editor.Action
import com.tunjid.heron.graze.editor.ActualGrazeEditorViewModel
import com.tunjid.heron.graze.editor.FilterNavigationEventInfo
import com.tunjid.heron.graze.editor.GrazeEditorScreen
import com.tunjid.heron.graze.editor.RouteViewModelInitializer
import com.tunjid.heron.graze.editor.State
import com.tunjid.heron.graze.editor.currentFilter
import com.tunjid.heron.graze.editor.ui.EditFeedInfoSheetState
import com.tunjid.heron.graze.editor.ui.Title
import com.tunjid.heron.graze.editor.ui.TopBarActions
import com.tunjid.heron.graze.editor.ui.rememberAddFilterSheetState
import com.tunjid.heron.graze.editor.ui.rememberEditFeedInfoSheetState
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
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.toRouteTrie
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_filter
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/graze/create"
private const val EditPattern = "/graze/edit/{feedGeneratorRecordKey}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

private val Route.feedGeneratorRecordKey by mappedRoutePath(
    mapper = ::RecordKey,
)

private val RequestTrie = mapOf(
    PathPattern(RoutePattern) to {
        null
    },
    PathPattern(EditPattern) to { route: Route ->
        Action.Update.InitialLoad(route.feedGeneratorRecordKey)
    },
).toRouteTrie()

internal val Route.initialLoad: Action.Update?
    get() = checkNotNull(RequestTrie[this]).invoke(this)

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

    @Provides
    @IntoMap
    @StringKey(EditPattern)
    fun provideEditRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = EditPattern,
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
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(EditPattern)
    fun provideEditPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    fun routePaneEntry(
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

            val addFilterSheetState = rememberAddFilterSheetState { addedFilter ->
                viewModel.accept(
                    Action.EditFilter.AddFilter(
                        path = state.currentPath,
                        filter = addedFilter,
                    ),
                )
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
                    val editFeedInfoSheetState =
                        rememberEditFeedInfoSheetState { name, description ->
                            viewModel.accept(
                                Action.Metadata(
                                    displayName = name,
                                    description = description,
                                ),
                            )
                        }
                    PoppableDestinationTopAppBar(
                        title = {
                            Title(
                                title = remember(
                                    state.currentPath,
                                    state.feedGenerator,
                                    state.grazeFeed,
                                    state.sharedElementPrefix,
                                ) {
                                    when (val feedGenerator = state.feedGenerator) {
                                        null -> Title.Pending(
                                            path = state.currentPath,
                                            recordKey = state.grazeFeed.recordKey,
                                            displayName = state.grazeFeed.displayName,
                                        )
                                        else -> Title.Created(
                                            path = state.currentPath,
                                            feedGenerator = feedGenerator,
                                            sharedElementPrefix = state.sharedElementPrefix,
                                        )
                                    }
                                },
                                paneScaffoldState = this,
                                onTitleClicked = {
                                    editFeedInfoSheetState.editFeed(state)
                                },
                            )
                        },
                        actions = {
                            TopBarActions(
                                grazeFeed = state.grazeFeed,
                                enabled = !state.isLoading,
                                onEditClicked = {
                                    editFeedInfoSheetState.editFeed(state)
                                },
                                onSaveClicked = {
                                    viewModel.accept(
                                        Action.Update.Save(
                                            feed = state.grazeFeed,
                                        ),
                                    )
                                },
                                onDeleteClicked = {
                                    viewModel.accept(
                                        Action.Update.Delete(state.grazeFeed.recordKey),
                                    )
                                },
                            )
                        },
                        onBackPressed = {
                            viewModel.accept(
                                if (state.currentPath.isNotEmpty()) Action.EditorNavigation.ExitFilter
                                else Action.Navigate.Pop,
                            )
                        },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        text = stringResource(Res.string.add_filter),
                        icon = Icons.Rounded.Add,
                        expanded = true,
                        enabled = !state.isLoading,
                        onClick = addFilterSheetState::show,
                    )
                },
                content = { contentPadding ->
                    GrazeEditorScreen(
                        modifier = Modifier
                            .padding(top = contentPadding.calculateTopPadding()),
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                    )
                },
            )

            NavigationBackHandler(
                state = rememberNavigationEventState(
                    currentInfo = remember(state.currentFilter) {
                        FilterNavigationEventInfo(state.currentFilter)
                    },
                ),
                isBackEnabled = state.currentPath.isNotEmpty(),
            ) {
                viewModel.accept(Action.EditorNavigation.ExitFilter)
            }
        },
    )
}

private fun EditFeedInfoSheetState.editFeed(
    state: State,
) {
    show(
        currentName = state.grazeFeed.displayName
            ?: state.feedGenerator?.displayName
            ?: "",
        currentDescription = state.grazeFeed.description
            ?: state.feedGenerator?.description,
    )
}
