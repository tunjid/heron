package com.tunjid.heron.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    activeProfileId: ProfileId?,
    sessionSummaries: List<SessionSummary>,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
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
            MultiSessionLayout(
                paneMovableElementSharedTransitionScope = paneScaffoldState,
                modifier = modifier,
                activeProfileId = activeProfileId,
                summaries = remember(sessionSummaries) {
                    when (sessionSummaries.size) {
                        in 0..MaxSessionsDisplayed -> sessionSummaries
                        else -> sessionSummaries.take(MaxSessionsDisplayed)
                    }
                },
                onAddAccountClick = onAddAccountClick,
                onAccountSelected = onAccountSelected,
            )
        }
    }
}

@Composable
fun MultiSessionLayout(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    modifier: Modifier = Modifier,
    activeProfileId: ProfileId?,
    summaries: List<SessionSummary>,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val onExpansionToggled = { isExpanded = !isExpanded }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
            movableContentWithReceiverOf<MovableElementSharedTransitionScope, Boolean> { expanded ->
                ExpandButton(
                    isExpanded = expanded,
                    onExpansionToggled = onExpansionToggled,
                )
            }
        }
        val sessionSummariesContent = remember {
            movableContentWithReceiverOf<
                MovableElementSharedTransitionScope,
                Boolean,
                ProfileId?,
                List<SessionSummary>,
                >
            { isExpanded, activeProfileId, summaries ->
                SessionSummaries(
                    isExpanded = isExpanded,
                    activeProfileId = activeProfileId,
                    sessionSummaries = summaries,
                    onAccountSelected = onAccountSelected,
                    onExpansionToggled = onExpansionToggled,
                )
            }
        }
        with(paneMovableElementSharedTransitionScope) {
            Box(
                modifier = Modifier
                    .animateBounds(this)
                    .clip(ExpandableAvatarRowShape),
            ) {
                if (isExpanded) ExpandedSummaries(
                    activeProfileId = activeProfileId,
                    summaries = summaries,
                    onExpansionToggled = onExpansionToggled,
                    settingsItemRow = settingsItemRow,
                    expandButtonContent = expandButtonContent,
                    sessionSummariesContent = sessionSummariesContent,
                )
                else CollapsedSummaries(
                    activeProfileId = activeProfileId,
                    summaries = summaries,
                    onExpansionToggled = onExpansionToggled,
                    settingsItemRow = settingsItemRow,
                    expandButtonContent = expandButtonContent,
                    sessionSummariesContent = sessionSummariesContent,
                )
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            visible = isExpanded,
        ) {
            SettingsItemRow(
                title = stringResource(Res.string.add_or_reauthenticate_account),
                icon = Icons.Default.PersonAdd,
                modifier = Modifier
                    .clickable(onClick = onAddAccountClick),
            )
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.CollapsedSummaries(
    activeProfileId: ProfileId?,
    summaries: List<SessionSummary>,
    onExpansionToggled: () -> Unit,
    settingsItemRow: @Composable () -> Unit,
    expandButtonContent: @Composable MovableElementSharedTransitionScope.(Boolean) -> Unit,
    sessionSummariesContent: @Composable MovableElementSharedTransitionScope.(Boolean, ProfileId?, List<SessionSummary>) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpansionToggled() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        settingsItemRow()
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverlappingAvatarRow(
                modifier = Modifier
                    .width(summaries.collapsedAvatarsWidth()),
                overlap = CollapsedAvatarOverlap,
                maxItems = summaries.size,
                content = {
                    sessionSummariesContent(false, activeProfileId, summaries)
                },
            )
            expandButtonContent(false)
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.ExpandedSummaries(
    activeProfileId: ProfileId?,
    summaries: List<SessionSummary>,
    onExpansionToggled: () -> Unit,
    settingsItemRow: @Composable () -> Unit,
    expandButtonContent: @Composable MovableElementSharedTransitionScope.(Boolean) -> Unit,
    sessionSummariesContent: @Composable MovableElementSharedTransitionScope.(Boolean, ProfileId?, List<SessionSummary>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpansionToggled() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            settingsItemRow()
            Box(
                Modifier
                    .padding(horizontal = 20.dp),
            ) {
                expandButtonContent(true)
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sessionSummariesContent(true, activeProfileId, summaries)
        }
    }
}

@Composable
private fun MovableElementSharedTransitionScope.ExpandButton(
    isExpanded: Boolean,
    onExpansionToggled: () -> Unit,
) {
    val rotation = animateFloatAsState(if (isExpanded) 180f else 0f)
    IconButton(
        modifier = Modifier
            .animateBounds(
                lookaheadScope = this@ExpandButton,
            )
            .size(32.dp)
            .graphicsLayer {
                rotationZ = rotation.value
            },
        onClick = {
            onExpansionToggled()
        },
        content = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun MovableElementSharedTransitionScope.SessionSummaries(
    isExpanded: Boolean,
    activeProfileId: ProfileId?,
    sessionSummaries: List<SessionSummary>,
    onAccountSelected: (SessionSummary) -> Unit,
    onExpansionToggled: () -> Unit,
) {
    sessionSummaries.forEach { summary ->
        Row(
            modifier = Modifier
                .animateBounds(lookaheadScope = this@SessionSummaries)
                .clickable {
                    if (isExpanded) onAccountSelected(summary)
                    else onExpansionToggled()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isActive = summary.profileId == activeProfileId

            PaneStickySharedElement(
                modifier = Modifier
                    .size(ExpandableAvatarSize),
                sharedContentState = rememberSharedContentState(summary.sharedElementKey),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap()
                        .clip(CircleShape),
                    args = ImageArgs(
                        url = summary.profileAvatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = summary.profileHandle.id,
                        shape = RoundedPolygonShape.Circle,
                    ),
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(),
                visible = isExpanded,
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        // Fill max width is needed so the text measuring doesn't cause
                        // animation glitches. This is also why the link is used for clicking
                        // as opposed to the full text.
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
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
fun AccountSwitchingTransitionLayer(
    paneScaffoldState: PaneScaffoldState,
    phase: AccountSwitchPhase,
    session: SessionSummary?,
) = with(paneScaffoldState) {
    if (phase == AccountSwitchPhase.IDLE || session == null) return@with

    val transition = updateTransition(phase, label = "accountSwitch")

    val dimAlpha by transition.animateFloat(label = "dim") {
        when (it) {
            AccountSwitchPhase.MORPHING -> 0.18f
            AccountSwitchPhase.SUCCESS -> 0.25f
            else -> 0f
        }
    }

    val scaleState = transition.animateFloat(
        label = "scale",
        transitionSpec = {
            when (targetState) {
                AccountSwitchPhase.SUCCESS ->
                    spring(dampingRatio = 0.5f, stiffness = 400f)

                else ->
                    tween(220, easing = FastOutSlowInEasing)
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = dimAlpha))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            PaneSharedElement(
                sharedContentState = rememberSharedContentState("avatar_${session.profileId}"),
            ) {
                AsyncImage(
                    args = remember(session.profileAvatar) {
                        ImageArgs(
                            url = session.profileAvatar?.uri,
                            contentDescription = session.profileHandle.id,
                            shape = RoundedPolygonShape.Circle,
                            contentScale = ContentScale.Crop,
                        )
                    },
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = scaleState.value
                            scaleY = scaleState.value
                        },
                )
            }

            AccountSwitchIndicator(
                phase = phase,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountSwitchIndicator(
    phase: AccountSwitchPhase,
    modifier: Modifier = Modifier,
) {
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
private fun SuccessCheckmarkOverlay(
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "checkScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

private val SessionSummary.sharedElementKey: String
    get() = "avatar-$profileId"

private fun List<SessionSummary>.collapsedAvatarsWidth(): Dp =
    size * (ExpandableAvatarSize - CollapsedAvatarOverlap)

private val CollapsedAvatarOverlap = 12.dp

private val ExpandableAvatarSize = 32.dp
private val ExpandableAvatarRowShape = RoundedCornerShape(8.dp)

private const val MaxSessionsDisplayed = 6
