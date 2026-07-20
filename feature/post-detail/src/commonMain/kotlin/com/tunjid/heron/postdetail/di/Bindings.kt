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

package com.tunjid.heron.postdetail.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.canReply
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.postdetail.Action
import com.tunjid.heron.postdetail.PostDetailScreen
import com.tunjid.heron.postdetail.PostDetailStateHolder
import com.tunjid.heron.postdetail.PostDetailViewModelInitializer
import com.tunjid.heron.postdetail.canTranslate
import com.tunjid.heron.postdetail.ui.ThreadDisplayOptions
import com.tunjid.heron.sheets.rememberInferenceSheetState
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.post_detail.generated.resources.Res
import heron.feature.post_detail.generated.resources.reply
import heron.feature.post_detail.generated.resources.title
import heron.feature.post_detail.generated.resources.translate_post_text
import heron.ui.core.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/profile/{profileHandleOrId}/post/{postRecordKey}"
private const val RouteUriPattern = "/{profileHandleOrId}/app.bsky.feed.post/{postRecordKey}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

internal val Route.postRecordKey by mappedRoutePath(
    mapper = ::RecordKey,
)

internal val Route.profileHandleOrId by mappedRoutePath(
    mapper = ::ProfileHandleOrId,
)

@BindingContainer
object PostDetailNavigationBindings {

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
    @StringKey(RouteUriPattern)
    fun provideRouteUriMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RouteUriPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class PostDetailBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @ClassKey(PostDetailStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: PostDetailViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        navigationContentTransformer = navigationContentTransformer,
    )

    @Provides
    @IntoMap
    @StringKey(RouteUriPattern)
    fun provideUriPaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry<Route>(
        contentTransform = navigationContentTransformer::contentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            Route(
                route = routeParser.hydrate(route),
                paneScaffoldState = rememberPaneScaffoldState(),
            )
        },
    )
}

@Composable
internal fun Route(
    route: Route,
    paneScaffoldState: PaneScaffoldState,
) {
    val stateHolder = paneScaffoldState.retainRouteStateHolder<PostDetailStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val topAppBarNestedScrollConnection =
        paneScaffoldState.topAppBarNestedScrollConnection

    val bottomNavigationNestedScrollConnection =
        paneScaffoldState.bottomNavigationNestedScrollConnection

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
            .nestedScroll(topAppBarNestedScrollConnection)
            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                nestedScroll(bottomNavigationNestedScrollConnection)
            },
        showNavigation = true,
        snackBarMessages = state.messages,
        onSnackBarMessageConsumed = {
            stateHolder.accept(Action.SnackbarDismissed(it))
        },
        topBar = {
            PoppableDestinationTopAppBar(
                transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                title = {
                    AppBarTitle(
                        title = stringResource(Res.string.title),
                    )
                },
                onBackPressed = { stateHolder.accept(Action.Navigate.Pop) },
                actions = {
                    val inferenceSheetState = rememberInferenceSheetState()
                    if (state.canTranslate) AppBarIconButton(
                        icon = Icons.Rounded.Translate,
                        iconDescription = stringResource(Res.string.translate_post_text),
                        onClick = click@{
                            val post = state.anchorPost ?: return@click
                            val postLanguageTag = state.postLanguageTag ?: return@click
                            val currentLanguageTag = state.currentLanguageTag ?: return@click
                            inferenceSheetState.translate(
                                post = post,
                                sourceLanguage = postLanguageTag,
                                targetLanguage = currentLanguageTag,
                            )
                        },
                    )
                    ThreadDisplayOptions(
                        modifier = Modifier
                            .padding(horizontal = 8.dp),
                        order = state.order,
                        viewMode = state.viewMode,
                        onOrderChanged = {
                            stateHolder.accept(Action.Load.Order(it))
                        },
                        onViewModeChanged = {
                            stateHolder.accept(Action.Load.ViewMode(it))
                        },
                    )
                },
            )
        },
        snackBarHost = {
            PaneSnackbarHost(
                modifier = Modifier
                    .offset {
                        fabOffset(bottomNavigationNestedScrollConnection.offset)
                    },
            )
        },
        floatingActionButton = {
            if (state.anchorPost?.viewerStats.canReply) PaneFab(
                modifier = Modifier
                    .offset {
                        fabOffset(bottomNavigationNestedScrollConnection.offset)
                    },
                text = stringResource(
                    when {
                        isSignedOut -> CommonStrings.sign_in
                        else -> Res.string.reply
                    },
                ),
                icon = when {
                    isSignedOut -> Icons.AutoMirrored.Rounded.Login
                    else -> Icons.AutoMirrored.Rounded.Reply
                },
                expanded = isFabExpanded {
                    if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset
                    else topAppBarNestedScrollConnection.offset * -1f
                },
                onClick = onClick@{
                    val anchorPost = state.anchorPost ?: return@onClick
                    stateHolder.accept(
                        Action.Navigate.To(
                            when {
                                isSignedOut -> signInDestination()
                                else -> composePostDestination(
                                    type = Post.Create.Reply(
                                        parent = anchorPost,
                                    ),
                                    sharedElementPrefix = state.sharedElementPrefix,
                                )
                            },
                        ),
                    )
                },
            )
        },
        navigationBar = {
            PaneNavigationBar(
                modifier = Modifier.offset {
                    bottomNavigationNestedScrollConnection.offset.round()
                },
            )
        },
        navigationRail = {
            PaneNavigationRail()
        },
        content = {
            PostDetailScreen(
                paneScaffoldState = this,
                state = state,
                actions = stateHolder.accept,
                modifier = Modifier,
            )
            SecondaryPaneCloseBackHandler()
        },
    )
}
