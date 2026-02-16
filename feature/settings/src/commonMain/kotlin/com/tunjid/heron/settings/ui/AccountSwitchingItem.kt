package com.tunjid.heron.settings.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.settings.AccountSwitchPhase
import com.tunjid.heron.ui.OverlappingAvatarRow
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.add_another_account
import heron.feature.settings.generated.resources.switch_account
import heron.ui.core.generated.resources.collapse_icon
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountSwitchingItem(
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
                modifier = Modifier.clickable(onClick = onAddAccountClick),
            )
        }
        else -> {
            ExpandableSettingsItemRow(
                title = stringResource(Res.string.switch_account),
                icon = Icons.Default.SwitchAccount,
                trailingContent = { isExpanded ->
                    if (isExpanded) {
                        ExpandCollapseIcon(isExpanded)
                    } else {
                        AccountAvatarStack(sessionSummaries)
                    }
                },
            ) {
                Column {
                    sessionSummaries.forEach { session ->
                        AccountRow(
                            paneScaffoldState = paneScaffoldState,
                            activeProfileId = activeProfileId,
                            session = session,
                            onClick = { onAccountSelected(session) },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItemRow(
                        title = stringResource(Res.string.add_another_account),
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.clickable(onClick = onAddAccountClick),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    paneScaffoldState: PaneScaffoldState,
    activeProfileId: ProfileId?,
    session: SessionSummary,
    onClick: () -> Unit,
) = with(paneScaffoldState) {
    val isActive = session.profileId == activeProfileId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = session.profileHandle.id,
            style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun ExpandCollapseIcon(isExpanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = spring(Spring.StiffnessMediumLow),
    )

    Icon(
        modifier = Modifier.graphicsLayer { rotationX = rotation },
        imageVector = Icons.Default.ExpandLess,
        contentDescription = stringResource(CommonStrings.collapse_icon),
    )
}

@Composable
private fun AccountAvatarStack(
    sessions: List<SessionSummary>,
) {
    OverlappingAvatarRow(
        modifier = Modifier.width(56.dp),
        overlap = 12.dp,
        maxItems = minOf(sessions.size, 3),
    ) {
        sessions.take(3).forEachIndexed { index, session ->
            AsyncImage(
                modifier = Modifier
                    .size(28.dp)
                    .zIndex(-index.toFloat()),
                args = remember(session.profileAvatar) {
                    ImageArgs(
                        url = session.profileAvatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = session.profileHandle.id,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
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

    val scale by transition.animateFloat(
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
                            scaleX = scale
                            scaleY = scale
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
