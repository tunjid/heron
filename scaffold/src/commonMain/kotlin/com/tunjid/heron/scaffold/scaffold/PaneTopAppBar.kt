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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.ui.core.generated.resources.go_back
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.RootDestinationTopAppBar(
    modifier: Modifier = Modifier,
    signedInProfile: Profile?,
    title: @Composable () -> Unit = {},
    transparencyFactor: () -> Float = { 0f },
    onSignedInProfileClicked: (Profile, String) -> Unit,
) {
    TopAppBar(
        modifier = modifier
            .rootAppBarBackground(
                backgroundColor = MaterialTheme.colorScheme.surface,
                progress = transparencyFactor,
            )
            .rootAppBarBlur(transparencyFactor),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            AppLogo(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp),
            )
        },
        title = title,
        actions = {
            AnimatedVisibility(
                visible = signedInProfile != null,
            ) {
                signedInProfile?.let { profile ->
                    PaneStickySharedElement(
                        modifier = Modifier
                            .size(36.dp),
                        sharedContentState = rememberSharedContentState(
                            key = SignedInUserAvatarSharedElementKey,
                        ),
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .fillParentAxisIfFixedOrWrap()
                                .clickable {
                                    onSignedInProfileClicked(
                                        profile,
                                        SignedInUserAvatarSharedElementKey,
                                    )
                                },
                            args = remember(profile) {
                                ImageArgs(
                                    url = profile.avatar?.uri,
                                    contentDescription = signedInProfile.displayName,
                                    contentScale = ContentScale.Crop,
                                    shape = RoundedPolygonShape.Circle,
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        },
    )
}

@Composable
fun PaneScaffoldState.PoppableDestinationTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    transparencyFactor: () -> Float = { 0f },
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        modifier = modifier
            .renderInSharedTransitionScopeOverlay(
                zIndexInOverlay = PoppableDestinationTopAppBarSharedElementZIndex,
                renderInOverlay = {
                    paneState.pane == ThreePane.Primary &&
                        isTransitionActive &&
                        isActive
                },
            )
            .rootAppBarBackground(
                backgroundColor = MaterialTheme.colorScheme.surface,
                progress = transparencyFactor,
            ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            AnimatedVisibility(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                visible = paneState.pane == ThreePane.Primary,
                enter = BackArrowEnter,
                exit = BackArrowExit,
                content = {
                    FilledTonalIconButton(
                        modifier = Modifier,
                        onClick = onBackPressed,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(CommonStrings.go_back),
                        )
                    }
                },
            )
        },
        title = {
            Box(
                modifier = Modifier,
            ) {
                title()
            }
        },
        actions = actions,
    )
}

@Suppress("UnusedReceiverParameter")
fun PaneScaffoldState.fullAppbarTransparency() = Float.NaN

@Composable
fun AppBarTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
}

private fun Modifier.rootAppBarBackground(
    backgroundColor: Color,
    progress: () -> Float,
): Modifier = drawBehind {
    drawRect(
        color = backgroundColor,
        alpha = when (val currentProgress = progress()) {
            in 0f..1f -> HundredPercent - (currentProgress * MaxTransparency)
            else -> 0F
        },
    )
}

private fun Modifier.rootAppBarBlur(
    progress: () -> Float,
): Modifier = blur(
    shape = RectangleShape,
    radius = ::RootAppBarBlurRadius,
    progress = progress,
)

private val BackArrowEnter: EnterTransition = slideInHorizontally { -it }
private val BackArrowExit: ExitTransition = slideOutHorizontally { -it }

private val RootAppBarBlurRadius = 60.dp

private const val SignedInUserAvatarSharedElementKey = "self"
private const val PoppableDestinationTopAppBarSharedElementZIndex = 10f
private const val MaxTransparency = 0.1f
private const val HundredPercent = 1f
