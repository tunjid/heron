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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.profile.AvatarLiveZIndex
import com.tunjid.heron.profile.AvatarZIndex
import com.tunjid.heron.profile.ProfileLiveChip
import com.tunjid.heron.profile.profileLiveAvatarBorder
import com.tunjid.heron.profile.withProfileAvatarLiveSharedElementPrefix
import com.tunjid.heron.scaffold.identity.IdentityAction
import com.tunjid.heron.scaffold.identity.IdentityState
import com.tunjid.heron.scaffold.identity.isStable
import com.tunjid.heron.scaffold.scaffold.components.ClickPassThroughToolbar
import com.tunjid.heron.ui.AppBarElevatedCard
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.AppBarTextButton
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.modifiers.shapedCombinedClickable
import com.tunjid.heron.ui.platformStatusBars
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.EmphasizedSingleLineOutlinedText
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.identity_account_add
import heron.scaffold.generated.resources.identity_account_switch_reauth
import heron.scaffold.generated.resources.identity_account_switching
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.RootDestinationTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
    transparencyFactor: () -> Float = { 0f },
    onSignedInProfileClicked: (Profile, String) -> Unit,
    onLogoClicked: (() -> Unit)? = null,
) {
    val identityState = appState.identityState
    ClickPassThroughToolbar(
        modifier = modifier
            .constrainedSizePlacement(
                orientation = Orientation.Horizontal,
                minSize = splitPaneState.minPaneWidth,
                atStart = splitPaneState.filteredPaneOrder.firstOrNull() == paneState.pane,
            )
            .rootAppBarBackground(
                backgroundColor = MaterialTheme.colorScheme.surface,
                progress = transparencyFactor,
            )
            .blur(
                shape = RectangleShape,
                radius = UiTokens::appBarBlurRadius,
                progress = transparencyFactor,
            ),
        windowInsets = WindowInsets.platformStatusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            AppLogo(
                modifier = Modifier
                    .padding(4.dp)
                    .then(
                        if (onLogoClicked != null && appState.identityState.isStable) {
                            Modifier.shapedClickable(
                                CircleShape,
                                onClick = onLogoClicked,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(4.dp)
                    .size(UiTokens.avatarSize),
                presentation = LogoPresentation.Destination.Root(
                    blurProgress = transparencyFactor,
                ),
            )
        },
        title = {
            AnimatedContent(
                modifier = Modifier
                    .appbarAnimatedBounds(),
                targetState = identityState.switchStatus,
                transitionSpec = {
                    TitleTransform
                },
            ) { currentState ->
                SwitchStatus(
                    currentState = currentState,
                    title = title,
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = identityState.isStable,
            ) {
                if (actions != null) actions()
            }
            LazyRow(
                modifier = Modifier
                    .appbarAnimatedBounds()
                    .clip(CircleShape),
                reverseLayout = true,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = identityState.pastSessions
                        .filter(identityState::canShow),
                    key = { it.profileId.id },
                    itemContent = { sessionSummary ->
                        this@RootDestinationTopAppBar.SessionAvatar(
                            modifier = Modifier
                                .animateItem(),
                            isLive = false,
                            profileAvatar = sessionSummary.profileAvatar,
                            profileDescription = null,
                            profileId = sessionSummary.profileId,
                            signedInProfileId = identityState.signedInProfile?.did,
                            onLongClick = {
                                if (identityState.pastSessions.isNotEmpty()) appState.onIdentityAction(
                                    IdentityAction.Switch.Choose,
                                )
                            },
                            onClick = {
                                when (identityState.switchStatus) {
                                    IdentityState.SwitchStatus.Choosing -> appState.onIdentityAction(
                                        IdentityAction.Switch.Transition(sessionSummary),
                                    )
                                    is IdentityState.SwitchStatus.Stable -> identityState.signedInProfile?.let { profile ->
                                        onSignedInProfileClicked(
                                            profile,
                                            UiTokens.SignedInUserAvatarSharedElementKey,
                                        )
                                    }
                                    is IdentityState.SwitchStatus.Switching -> Unit
                                }
                            },
                        )
                    },
                )
            }
            AnimatedVisibility(
                modifier = Modifier
                    .appbarAnimatedBounds(),
                visible = identityState.switchStatus is IdentityState.SwitchStatus.Choosing,
            ) {
                AppBarIconButton(
                    onClick = {
                        appState.onIdentityAction(IdentityAction.Switch.Cancel)
                    },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color.Transparent,
                    ),
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = null,
                        )
                    },
                )
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
    ClickPassThroughToolbar(
        modifier = modifier
            .constrainedSizePlacement(
                orientation = Orientation.Horizontal,
                minSize = splitPaneState.minPaneWidth,
                atStart = splitPaneState.filteredPaneOrder.firstOrNull() == paneState.pane,
            )
            .renderInSharedTransitionScopeOverlay(
                zIndexInOverlay = UiTokens.appBarSharedElementZIndex,
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
        windowInsets = WindowInsets.platformStatusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            AppLogo(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackPressed)
                    .size(UiTokens.avatarSize),
                presentation = LogoPresentation.Destination.Poppable,
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

@Composable
private fun PaneScaffoldState.SwitchStatus(
    currentState: IdentityState.SwitchStatus,
    title: @Composable () -> Unit,
) {
    when (currentState) {
        is IdentityState.SwitchStatus.Stable.Idle -> title()
        is IdentityState.SwitchStatus.Stable.Error -> {
            val description = stringResource(Res.string.identity_account_switch_reauth)
            AppBarTextButton(
                modifier = Modifier
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
                onClick = appState::addAccount,
                content = {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    EmphasizedSingleLineOutlinedText(
                        text = description,
                    )
                },
            )
        }
        IdentityState.SwitchStatus.Choosing -> {
            val description = stringResource(Res.string.identity_account_add)
            AppBarTextButton(
                modifier = Modifier
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
                onClick = appState::addAccount,
                content = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    EmphasizedSingleLineOutlinedText(
                        text = description,
                    )
                },
            )
        }
        is IdentityState.SwitchStatus.Switching -> AppBarElevatedCard(
            content = {
                LoadingIndicator(
                    modifier = Modifier
                        .size(24.dp),
                )
                EmphasizedSingleLineOutlinedText(
                    text = stringResource(Res.string.identity_account_switching),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PaneScaffoldState.SessionAvatar(
    modifier: Modifier = Modifier,
    isLive: Boolean,
    profileAvatar: ImageUri?,
    profileDescription: String?,
    profileId: ProfileId,
    signedInProfileId: ProfileId?,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val isSignedInProfile = profileId == signedInProfileId
    Box(
        modifier = modifier
            .size(UiTokens.appBarButtonSize),
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize(),
            sharedContentState = rememberSharedContentState(
                key = when {
                    isSignedInProfile -> UiTokens.SignedInUserAvatarSharedElementKey
                    else -> profileId.id
                },
            ),
            zIndexInOverlay = AvatarZIndex,
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap()
                    .ifTrue(
                        predicate = isLive,
                        block = Modifier::profileLiveAvatarBorder,
                    )
                    .shapedCombinedClickable(
                        CircleShape,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    ),
                args = remember(profileAvatar) {
                    ImageArgs(
                        url = profileAvatar?.uri,
                        contentDescription = profileDescription,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
            if (isLive && isSignedInProfile) PaneStickySharedElement(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                sharedContentState = rememberSharedContentState(
                    key = UiTokens.SignedInUserAvatarSharedElementKey
                        .withProfileAvatarLiveSharedElementPrefix(),
                ),
                zIndexInOverlay = AvatarLiveZIndex,
            ) {
                ProfileLiveChip()
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
fun PaneScaffoldState.fullAppbarTransparency() = Float.NaN

@Composable
fun AppBarTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
}

private fun IdentityState.canShow(
    summary: SessionSummary,
): Boolean = when (val status = switchStatus) {
    IdentityState.SwitchStatus.Choosing -> true
    is IdentityState.SwitchStatus.Stable -> summary.profileId == signedInProfile?.did
    is IdentityState.SwitchStatus.Switching -> summary.profileId == status.session.profileId
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

context(paneScaffoldState: PaneScaffoldState)
private fun Modifier.appbarAnimatedBounds(): Modifier =
    animateBounds(
        lookaheadScope = paneScaffoldState,
        boundsTransform = paneScaffoldState.childBoundsTransform,
    )

private const val TitleAnimationMillis = 600
private val TitleAnimationSpec = tween<IntOffset>(TitleAnimationMillis)

private val TitleTransform = slideInVertically(
    animationSpec = TitleAnimationSpec,
    initialOffsetY = { it },
) togetherWith slideOutVertically(
    animationSpec = TitleAnimationSpec,
    targetOffsetY = { -it },
)

private const val MaxTransparency = 0.1f
private const val HundredPercent = 1f
