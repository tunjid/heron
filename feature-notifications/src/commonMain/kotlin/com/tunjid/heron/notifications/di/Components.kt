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

package com.tunjid.heron.notifications.di

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Badge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.notifications.Action
import com.tunjid.heron.notifications.ActualNotificationsViewModel
import com.tunjid.heron.notifications.NotificationsScreen
import com.tunjid.heron.notifications.NotificationsViewModelCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.AppStack
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_notifications.generated.resources.Res
import heron.feature_notifications.generated.resources.create_post
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/notifications"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@KmpComponentCreate
expect fun NotificationsNavigationComponent.Companion.create(): NotificationsNavigationComponent

@KmpComponentCreate
expect fun NotificationsComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): NotificationsComponent

@Component
abstract class NotificationsNavigationComponent {
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
abstract class NotificationsComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: NotificationsViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualNotificationsViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier,
                        signedInProfile = state.signedInProfile,
                        onSignedInProfileClicked = { profile, sharedElementKey ->
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToProfile(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = profile,
                                        avatarSharedElementKey = sharedElementKey,
                                    )
                                )
                            )
                        },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                if (isMediumScreenWidthOrWider) IntOffset.Zero
                                else bottomNavigationNestedScrollConnection.offset.round()
                            },
                        text = stringResource(Res.string.create_post),
                        icon = Icons.Rounded.Edit,
                        expanded = isFabExpanded(
                            offset = bottomNavigationNestedScrollConnection.offset
                        ),
                        onClick = {
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ComposePost(
                                        type = Post.Create.Timeline,
                                        sharedElementPrefix = null,
                                    )
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    PaneBottomAppBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.offset.round()
                            },
                        badge = { stack ->
                            if (stack == AppStack.Notifications && state.unreadNotificationCount != 0L) {
                                Badge(Modifier.size(4.dp))
                            }
                        },
                    )
                },
                content = {
                    NotificationsScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier,
                    )
                }
            )
        }
    )
}
