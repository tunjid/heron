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

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.ComposeScreen
import com.tunjid.heron.compose.ComposeStateHolder
import com.tunjid.heron.compose.ComposeViewModelInitializer
import com.tunjid.heron.compose.canDraft
import com.tunjid.heron.compose.drafts.DraftsStateHolder
import com.tunjid.heron.compose.drafts.DraftsViewModelInitializer
import com.tunjid.heron.compose.drafts.rememberDraftsSheetState
import com.tunjid.heron.compose.hasComposedContent
import com.tunjid.heron.compose.hasLongPost
import com.tunjid.heron.compose.ui.ComposePostBottomBar
import com.tunjid.heron.compose.ui.ComposePostFabRow
import com.tunjid.heron.compose.ui.TopAppBarFab
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.DestructiveDialogButton
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.rememberSimpleDialogState
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.discard
import heron.feature.compose.generated.resources.drafts
import heron.feature.compose.generated.resources.keep_editing
import heron.feature.compose.generated.resources.save_draft
import heron.feature.compose.generated.resources.save_draft_dialog_text
import heron.feature.compose.generated.resources.save_draft_dialog_title
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/compose"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object ComposeNavigationBindings {

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
class ComposeBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @ClassKey(ComposeStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: ComposeViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    // The drafts sheet is compose-only, so its sheet state holder is contributed here rather than
    // from the shared :ui:sheets SheetBindings. It merges into the same app-graph sheet map.
    @Provides
    @IntoMap
    @ClassKey(DraftsStateHolder::class)
    fun provideDraftsViewModelInitializer(
        initializer: DraftsViewModelInitializer,
    ): SheetStateHolderInitializer = SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry(
        contentTransform = navigationContentTransformer::contentTransform,
        render = { route ->
            Route(
                route = route,
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
    val stateHolder = paneScaffoldState.retainRouteStateHolder<ComposeStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val draftsSheetState = paneScaffoldState.rememberDraftsSheetState(
        onDraftSelected = { stateHolder.accept(Action.LoadDraft(it)) },
    )
    val discardDialogState = rememberSimpleDialogState()

    // Only interrupt leaving when there is unsaved content on a post that could become a draft.
    // Replies and quotes can't be drafted, so they pop immediately.
    val shouldOfferDraft = state.canDraft && state.hasComposedContent

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = shouldOfferDraft,
        onBackCompleted = discardDialogState::show,
    )

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState),
        showNavigation = true,
        snackBarMessages = state.messages,
        onSnackBarMessageConsumed = {
            stateHolder.accept(Action.SnackbarDismissed(it))
        },
        topBar = scope@{
            PoppableDestinationTopAppBar(
                actions = {
                    if (state.canDraft) AppBarIconButton(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = this@scope,
                                boundsTransform = this@scope.childBoundsTransform,
                            ),
                        icon = Icons.Rounded.Drafts,
                        iconDescription = stringResource(Res.string.drafts),
                        onClick = draftsSheetState::showDrafts,
                    )
                    Box(
                        modifier = Modifier
                            .ifTrue(state.hasLongPost) {
                                padding(horizontal = 8.dp)
                            }
                            .ifTrue(!state.hasLongPost) {
                                // Always has to be in composition, so make very narrow
                                requiredWidth(Dp.Hairline)
                            },
                    ) {
                        TopAppBarFab(
                            state = state,
                            onCreatePost = stateHolder.accept,
                        )
                    }
                },
                onBackPressed = {
                    if (shouldOfferDraft) discardDialogState.show()
                    else stateHolder.accept(Action.Navigate.Pop)
                },
            )
        },
        floatingActionButton = {
            ComposePostFabRow(
                modifier = Modifier,
                state = state,
                onAction = stateHolder.accept,
            )
        },
        navigationBar = {
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
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                postText = state.postText,
                photos = state.photos,
                onMediaEdited = stateHolder.accept,
            )

            DisposableEffect(hasBlankText, imeShowing) {
                val fabExpanded = hasBlankText || !imeShowing
                stateHolder.accept(Action.SetFabExpanded(expanded = fabExpanded))
                onDispose { }
            }
        },
        navigationRail = {
            PaneNavigationRail()
        },
        content = { paddingValues ->
            ComposeScreen(
                paneScaffoldState = this,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(
                        // This padding is solely for the post interaction button
                        bottom = UiTokens.toolbarHeight,
                    ),
                state = state,
                actions = stateHolder.accept,
            )
        },
    )

    SimpleDialog(
        state = discardDialogState,
        title = {
            SimpleDialogTitle(text = stringResource(Res.string.save_draft_dialog_title))
        },
        text = {
            SimpleDialogText(text = stringResource(Res.string.save_draft_dialog_text))
        },
        confirmButton = {
            PrimaryDialogButton(
                text = stringResource(Res.string.save_draft),
                onClick = {
                    discardDialogState.hide()
                    stateHolder.accept(Action.SaveDraft)
                },
            )
        },
        dismissButton = {
            Row {
                DestructiveDialogButton(
                    text = stringResource(Res.string.discard),
                    onClick = {
                        discardDialogState.hide()
                        stateHolder.accept(Action.Navigate.Pop)
                    },
                )
                NeutralDialogButton(
                    text = stringResource(Res.string.keep_editing),
                    onClick = discardDialogState::hide,
                )
            }
        },
    )
}
