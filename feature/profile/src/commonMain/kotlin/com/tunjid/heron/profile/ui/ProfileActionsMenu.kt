package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.models.isMuted
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.more_options
import heron.ui.core.generated.resources.viewer_state_block_account
import heron.ui.core.generated.resources.viewer_state_mute_account
import heron.ui.core.generated.resources.viewer_state_unmute_account
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileActionsMenu(
    modifier: Modifier = Modifier,
    items: List<ProfileActionMenu>,
    onItemClicked: (ProfileActionMenu.Item) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.wrapContentSize(Alignment.TopEnd),
    ) {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            shape = CircleShape,
            label = {}, // keep empty
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(CommonStrings.more_options),
                )
            },
            modifier = Modifier
                .size(35.dp)
                .clip(CircleShape),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 200.dp),
        ) {
            items.forEach { item ->
                when (item) {
                    is ProfileActionMenu.Item -> ProfileActionMenuItemRow(
                        item = item,
                        onClick = {
                            expanded = false
                            onItemClicked(item)
                        },
                    )
                    ProfileActionMenu.Divider -> HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileActionMenuItemRow(
    item: ProfileActionMenu.Item,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
        text = {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (item.isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .height(48.dp),
    )
}

@Stable
internal sealed class ProfileActionMenu {

    data class Item(
        val title: StringResource,
        val icon: ImageVector,
        val isDestructive: Boolean = false,
    ) : ProfileActionMenu()

    data object Divider : ProfileActionMenu()
}

internal fun ProfileViewerState?.profileActionMenuItems() = buildList {
    if (this@profileActionMenuItems != null) {
        if (!isBlocked) add(
            ProfileActionMenu.Item(
                title = CommonStrings.viewer_state_block_account,
                icon = Icons.Rounded.PersonOff,
                isDestructive = true,
            ),
        )

        add(
            if (isMuted) ProfileActionMenu.Item(
                title = CommonStrings.viewer_state_unmute_account,
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                isDestructive = true,
            )
            else ProfileActionMenu.Item(
                title = CommonStrings.viewer_state_mute_account,
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                isDestructive = false,
            ),
        )
    }
}
