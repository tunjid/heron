package com.tunjid.heron.timeline.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.ListCollectionShape
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.RecordBlurb
import com.tunjid.heron.ui.RecordSubtitle
import com.tunjid.heron.ui.RecordText
import com.tunjid.heron.ui.RecordTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.action_add
import heron.ui.core.generated.resources.action_remove
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.list_by_you
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListMemberPickerItem(
    modifier: Modifier = Modifier,
    list: FeedList,
    membership: ListMember?,
    profileId: ProfileId,
    onAddListMember: (ProfileId, ListUri) -> Unit,
    onRemoveListMember: (ListMemberUri) -> Unit,
) {
    ListPickerRecordLayout(
        modifier = modifier,
        title = list.name,
        subtitle = stringResource(Res.string.list_by_you),
        description = list.description,
        blurb = null,
        avatar = {
            val avatar = list.avatar ?: BlueskyClouds
            AsyncImage(
                modifier = Modifier.size(44.dp),
                args = remember(avatar) {
                    ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = ListCollectionShape,
                    )
                },
            )
        },
        action = {
            ListMembershipChip(
                isMember = membership != null,
                onClick = {
                    if (membership != null) onRemoveListMember(membership.uri)
                    else onAddListMember(profileId, list.uri)
                },
            )
        },
    )
}

@Composable
private fun ListMembershipChip(
    isMember: Boolean,
    onClick: () -> Unit,
) {
    val label = if (isMember) stringResource(CommonStrings.action_remove)
    else stringResource(CommonStrings.action_add)

    FilterChip(
        selected = isMember,
        onClick = onClick,
        shape = ListMemberChipShape,
        leadingIcon = {
            Icon(
                imageVector = if (isMember) Icons.Rounded.PlaylistRemove
                else Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = label,
            )
        },
        label = {
            Text(label)
        },
    )
}

@Composable
private fun ListPickerRecordLayout(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    description: String?,
    blurb: String?,
    avatar: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AttributionLayout(
            modifier = Modifier.fillMaxWidth(),
            avatar = avatar,
            label = {
                // Renders identically to RecordLayout's title/subtitle
                // but without PaneStickySharedElement wrapping —
                // bottom sheets are separate windows and cannot
                // participate in shared element transitions
                RecordTitle(title = title)
                RecordSubtitle(subtitle = subtitle)
            },
            action = action,
        )
        description.takeUnless(String?::isNullOrEmpty)?.let {
            RecordText(text = it)
        }
        blurb.takeUnless(String?::isNullOrEmpty)?.let {
            RecordBlurb(blurb = it)
        }
    }
}

private val ListMemberChipShape = RoundedCornerShape(16.dp)
