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

package com.tunjid.heron.home.di

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.home.Action
import com.tunjid.heron.home.ActualHomeStateHolder
import com.tunjid.heron.home.HomeScreen
import com.tunjid.heron.home.HomeStateHolderCreator
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.AppLogo
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.scaffold.requirePanedSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.threepane.configurations.requireThreePaneMovableSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides

internal const val RoutePattern = "/home"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@Component
abstract class HomeNavigationComponent {

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class HomeComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: HomeStateHolderCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualHomeStateHolder> {
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
                    TopBar(
                        movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
                        animatedVisibilityScope = this,
                        signedInProfile = state.signedInProfile,
                        onSignedInProfileClicked = {
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToProfile(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = it,
                                        avatarSharedElementKey = SignedInUserAvatarSharedElementKey,
                                    )
                                )
                            )
                        },
                    )
                },
                content = { paddingValues ->
                    HomeScreen(
                        sharedElementScope = requirePanedSharedElementScope(),
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(paddingValues = paddingValues)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                )
                            ),
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopBar(
    movableSharedElementScope: MovableSharedElementScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    signedInProfile: Profile?,
    onSignedInProfileClicked: (Profile) -> Unit,
) = with(movableSharedElementScope) {
    TopAppBar(
        navigationIcon = {
            Image(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(36.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(AppLogo),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(stiffness = Spring.StiffnessLow)
                        }
                    ),
                imageVector = AppLogo,
                contentDescription = null,
            )
        },
        title = {

        },
        actions = {
            AnimatedVisibility(
                visible = signedInProfile != null
            ) {
                signedInProfile?.let { profile ->
                    AsyncImage(
                        modifier = Modifier
                            .size(24.dp)
                            .sharedElement(
                                state = rememberSharedContentState(
                                    SignedInUserAvatarSharedElementKey
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                            .clickable { onSignedInProfileClicked(profile) },
                        args = remember(profile) {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentDescription = signedInProfile.displayName,
                                contentScale = ContentScale.Crop,
                                shape = ImageShape.Circle,
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        },
    )
}

private const val SignedInUserAvatarSharedElementKey = "self"