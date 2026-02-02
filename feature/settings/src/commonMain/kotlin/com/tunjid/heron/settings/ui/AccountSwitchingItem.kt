package com.tunjid.heron.settings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
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
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.add_another_account
import heron.feature.settings.generated.resources.switch_account
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountSwitchingItem(
    sessionSummaries: List<SessionSummary>,
    onAddAccountClick: () -> Unit,
    onAccountSelected: (SessionSummary) -> Unit,
) {
    if (sessionSummaries.size <= 1) {
        SettingsItemRow(
            title = stringResource(Res.string.add_another_account),
            icon = Icons.Default.PersonAdd,
            modifier = Modifier.clickable(onClick = onAddAccountClick),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
            )
        }
        return
    }

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
        LazyColumn(
            modifier = Modifier.height(150.dp),
        ) {
            items(
                items = sessionSummaries,
                key = { session -> session.profileId },
            ) { session ->
                AccountRow(
                    session = session,
                    onClick = { onAccountSelected(session) },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsItemRow(
                    title = stringResource(Res.string.add_another_account),
                    icon = Icons.Default.PersonAdd,
                    modifier = Modifier
                        .clickable(onClick = onAddAccountClick),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
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
        contentDescription = null,
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
