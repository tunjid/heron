package com.tunjid.heron.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.settings.AccountSwitchPhase
import com.tunjid.heron.ui.OverlappingAvatarRow
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.add_another_account
import heron.feature.settings.generated.resources.add_or_reauthenticate_account
import heron.feature.settings.generated.resources.switch_account
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountSwitchingItem(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    switchPhase: AccountSwitchPhase,
    activeProfileId: ProfileId?,
    sessionSummaries: List<SessionSummary>,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
    switchingSession: SessionSummary?,
) {
    when {
        sessionSummaries.size <= 1 -> {
            SettingsItemRow(
                title = stringResource(Res.string.add_another_account),
                icon = Icons.Default.PersonAdd,
                modifier = modifier.clickable(onClick = onAddAccountClick),
            )
        }
        else -> {
            val state =
                rememberAnimationData(
                    activeProfileId = activeProfileId,
                    sessionSummaries =
                        remember(sessionSummaries) {
                            when (sessionSummaries.size) {
                                in 0..MaxSessionsDisplayed -> sessionSummaries
                                else -> sessionSummaries.take(MaxSessionsDisplayed)
                            }
                        },
                    switchingSession = switchingSession,
                    switchPhase = switchPhase,
                )
            AnimatedContent(targetState = switchingSession) { session ->
                if (session == null)
                    MultiSessionLayout(
                        paneMovableElementSharedTransitionScope = paneScaffoldState,
                        animatedVisibilityScope = this,
                        modifier = modifier,
                        accountSwitchState = state,
                        onAddAccountClick = onAddAccountClick,
                        onAccountSelected = onAccountSelected,
                    )
                else
                    AccountSwitchingTransitionLayer(
                        modifier = Modifier.fillMaxSize(),
                        paneScaffoldState = paneScaffoldState,
                        animatedVisibilityScope = this,
                        phase = switchPhase,
                        session = session,
                    )
            }
        }
    }
}

@Composable
private fun MultiSessionLayout(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    accountSwitchState: AccountSwitchState,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
) {
    val onExpansionToggled = { accountSwitchState.isExpanded = !accountSwitchState.isExpanded }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val settingsItemRow = remember {
            movableContentOf {
                SettingsItemRow(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.switch_account),
                    icon = Icons.Default.SwitchAccount,
                ) {}
            }
        }
        val expandButtonContent = remember {
            movableContentWithReceiverOf<MovableElementSharedTransitionScope, AccountSwitchState> {
                state ->
                ExpandButton(accountSwitchState = state, onExpansionToggled = onExpansionToggled)
            }
        }
        val sessionSummariesContent = remember {
            movableContentWithReceiverOf<
                MovableElementSharedTransitionScope,
                AccountSwitchState,
                AnimatedVisibilityScope,
            > { state, scope ->
                SessionSummaries(
                    animatedVisibilityScope = scope,
                    accountSwitchState = state,
                    onAccountSelected = onAccountSelected,
                    onExpansionToggled = onExpansionToggled,
                )
            }
        }
        with(paneMovableElementSharedTransitionScope) {
            Box(modifier = Modifier.animateBounds(this).clip(ExpandableAvatarRowShape)) {
                if (accountSwitchState.isExpanded)
                    ExpandedSummaries(
                        animatedVisibilityScope = animatedVisibilityScope,
                        accountSwitchState = accountSwitchState,
                        onExpansionToggled = onExpansionToggled,
                        settingsItemRow = settingsItemRow,
                        expandButtonContent = expandButtonContent,
                        sessionSummariesContent = sessionSummariesContent,
                    )
                else
                    CollapsedSummaries(
                        animatedVisibilityScope = animatedVisibilityScope,
                        accountSwitchState = accountSwitchState,
                        onExpansionToggled = onExpansionToggled,
                        settingsItemRow = settingsItemRow,
                        expandButtonContent = expandButtonContent,
                        sessionSummariesContent = sessionSummariesContent,
                    )
            }
        }
        AnimatedVisibility(
            modifier = Modifier.padding(horizontal = 24.dp),
            visible = accountSwitchState.isExpanded,
        ) {
            SettingsItemRow(
                title = stringResource(Res.string.add_or_reauthenticate_account),
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.clickable(onClick = onAddAccountClick),
            )
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.CollapsedSummaries(
    animatedVisibilityScope: AnimatedVisibilityScope,
    accountSwitchState: AccountSwitchState,
    onExpansionToggled: () -> Unit,
    settingsItemRow: @Composable () -> Unit,
    expandButtonContent:
        @Composable
        MovableElementSharedTransitionScope.(AccountSwitchState) -> Unit,
    sessionSummariesContent:
        @Composable
        MovableElementSharedTransitionScope.(AccountSwitchState, AnimatedVisibilityScope) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onExpansionToggled() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        settingsItemRow()
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverlappingAvatarRow(
                modifier =
                    Modifier.width(accountSwitchState.sessionSummaries.collapsedAvatarsWidth()),
                overlap = CollapsedAvatarOverlap,
                maxItems = accountSwitchState.sessionSummaries.size,
                content = { sessionSummariesContent(accountSwitchState, animatedVisibilityScope) },
            )
            expandButtonContent(accountSwitchState)
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.ExpandedSummaries(
    animatedVisibilityScope: AnimatedVisibilityScope,
    accountSwitchState: AccountSwitchState,
    onExpansionToggled: () -> Unit,
    settingsItemRow: @Composable () -> Unit,
    expandButtonContent:
        @Composable
        MovableElementSharedTransitionScope.(AccountSwitchState) -> Unit,
    sessionSummariesContent:
        @Composable
        MovableElementSharedTransitionScope.(AccountSwitchState, AnimatedVisibilityScope) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onExpansionToggled() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            settingsItemRow()
            Box(Modifier.padding(horizontal = 20.dp)) { expandButtonContent(accountSwitchState) }
        }
        Column(
            modifier = Modifier.padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sessionSummariesContent(accountSwitchState, animatedVisibilityScope)
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.ExpandButton(
    accountSwitchState: AccountSwitchState,
    onExpansionToggled: () -> Unit,
) {
    val rotation = animateFloatAsState(if (accountSwitchState.isExpanded) 180f else 0f)
    IconButton(
        modifier =
            Modifier.animateBounds(lookaheadScope = this@ExpandButton).size(32.dp).graphicsLayer {
                rotationZ = rotation.value
            },
        onClick = { onExpansionToggled() },
        content = { Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
    )
}

@Composable
private fun MovableElementSharedTransitionScope.SessionSummaries(
    animatedVisibilityScope: AnimatedVisibilityScope,
    accountSwitchState: AccountSwitchState,
    onAccountSelected: (SessionSummary) -> Unit,
    onExpansionToggled: () -> Unit,
) {
    accountSwitchState.sessionSummaries.forEach { summary ->
        Row(
            modifier =
                Modifier.animateBounds(lookaheadScope = this@SessionSummaries).clickable {
                    if (accountSwitchState.isExpanded) onAccountSelected(summary)
                    else onExpansionToggled()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isActive = summary.profileId == accountSwitchState.activeProfileId
            AsyncImage(
                modifier =
                    Modifier.sharedElement(
                            sharedContentState =
                                rememberSharedContentState(summary.sharedElementKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .size(ExpandableAvatarSize)
                        .clip(CircleShape),
                args =
                    remember(summary.profileAvatar) {
                        ImageArgs(
                            url = summary.profileAvatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = summary.profileHandle.id,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
            )

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = accountSwitchState.isExpanded,
                exit = fadeOut(),
            ) {
                Row(
                    modifier =
                        Modifier
                            // Fill max width is needed so the text measuring doesn't cause
                            // animation glitches.
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                ) {
                    Text(
                        modifier = Modifier,
                        text = summary.profileHandle.id,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Visible,
                        maxLines = 1,
                    )
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSwitchingTransitionLayer(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    phase: AccountSwitchPhase,
    session: SessionSummary,
) =
    with(paneScaffoldState) {
        val transition = updateTransition(phase, label = "accountSwitch")

        val scale by
            transition.animateFloat(
                label = "scale",
                transitionSpec = {
                    when (targetState) {
                        AccountSwitchPhase.SUCCESS -> spring(dampingRatio = 0.5f, stiffness = 400f)

                        else -> tween(220, easing = FastOutSlowInEasing)
                    }
                },
            ) {
                when (it) {
                    AccountSwitchPhase.MORPHING -> 1.15f
                    AccountSwitchPhase.SUCCESS -> 1.2f
                    else -> 1f
                }
            }

        val haptic = LocalHapticFeedback.current
        LaunchedEffect(phase) {
            if (phase == AccountSwitchPhase.SUCCESS) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        Box(
            modifier =
                modifier.padding(vertical = 32.dp).fillMaxSize().clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PaneSharedElement(
                    modifier = Modifier.size(88.dp * scale),
                    sharedContentState =
                        rememberSharedContentState(
                            key = UiTokens.SignedInUserAvatarSharedElementKey
                        ),
                ) {
                    AsyncImage(
                        args =
                            remember(session.profileAvatar) {
                                ImageArgs(
                                    url = session.profileAvatar?.uri,
                                    contentDescription = session.profileHandle.id,
                                    shape = RoundedPolygonShape.Circle,
                                    contentScale = ContentScale.Crop,
                                )
                            },
                        modifier =
                            Modifier.sharedElement(
                                    sharedContentState =
                                        rememberSharedContentState(session.sharedElementKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                .fillParentAxisIfFixedOrWrap(),
                    )
                }

                AccountSwitchIndicator(phase = phase, modifier = Modifier.matchParentSize())
            }
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountSwitchIndicator(phase: AccountSwitchPhase, modifier: Modifier = Modifier) {
    when (phase) {
        AccountSwitchPhase.LOADING -> {
            CircularWavyProgressIndicator(modifier.fillMaxSize())
        }

        AccountSwitchPhase.SUCCESS -> {
            SuccessCheckmarkOverlay(modifier)
        }

        else -> Unit
    }
}

@Composable
private fun SuccessCheckmarkOverlay(modifier: Modifier = Modifier) {
    val scale by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
            label = "checkScale",
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier.size(36.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Composable
private fun rememberAnimationData(
    activeProfileId: ProfileId?,
    sessionSummaries: List<SessionSummary>,
    switchingSession: SessionSummary?,
    switchPhase: AccountSwitchPhase,
): AccountSwitchState {
    return remember {
            AccountSwitchState(
                activeProfileId = activeProfileId,
                sessionSummaries = sessionSummaries,
                switchingSession = switchingSession,
                switchPhase = switchPhase,
            )
        }
        .also {
            it.activeProfileId = activeProfileId
            it.sessionSummaries = sessionSummaries
            it.switchingSession = switchingSession
            it.switchPhase = switchPhase
        }
}

@Stable
private class AccountSwitchState(
    activeProfileId: ProfileId?,
    sessionSummaries: List<SessionSummary>,
    switchingSession: SessionSummary?,
    switchPhase: AccountSwitchPhase,
) {
    var isExpanded by mutableStateOf(false)
    var activeProfileId by mutableStateOf(activeProfileId)
    var sessionSummaries by mutableStateOf(sessionSummaries)
    var switchingSession by mutableStateOf(switchingSession)
    var switchPhase by mutableStateOf(switchPhase)
}

private val SessionSummary.sharedElementKey: String
    get() = "avatar-$profileId"

private fun List<SessionSummary>.collapsedAvatarsWidth(): Dp =
    size * (ExpandableAvatarSize - CollapsedAvatarOverlap)

private val CollapsedAvatarOverlap = 12.dp

private val ExpandableAvatarSize = 32.dp
private val ExpandableAvatarRowShape = RoundedCornerShape(8.dp)

private const val MaxSessionsDisplayed = 6
