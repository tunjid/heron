package com.tunjid.heron.settings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
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
    isSwitchingAccount: Boolean,
    sessionSummaries: List<SessionSummary>,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
) {
    when {
        sessionSummaries.size <= 1 -> {
            SettingsItemRow(
                title = stringResource(Res.string.add_another_account),
                icon = Icons.Default.PersonAdd,
                enabled = isSwitchingAccount,
                modifier = Modifier.clickable(onClick = onAddAccountClick),
            )
        }

        else -> {
            ExpandableSettingsItemRow(
                title = stringResource(Res.string.switch_account),
                icon = Icons.Default.SwitchAccount,
                enabled = true,
                trailingContent = { isExpanded ->
                    when {
                        isSwitchingAccount && sessionSummaries.isNotEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        isExpanded -> {
                            ExpandCollapseIcon(isExpanded)
                        }
                        else -> {
                            AccountAvatarStack(sessionSummaries)
                        }
                    }
                },

            ) {
                Column {
                    sessionSummaries.forEach { session ->
                        AccountRow(
                            session = session,
                            onClick = { onAccountSelected(session) },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItemRow(
                        title = stringResource(Res.string.add_another_account),
                        icon = Icons.Default.PersonAdd,
                        enabled = true,
                        modifier = Modifier.clickable(onClick = onAddAccountClick),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    session: SessionSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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

        Text(
            text = session.profileHandle.id,
            style = MaterialTheme.typography.bodyMedium,
        )
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
