package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.profile.ProfileScreenStateHolders
import com.tunjid.heron.timeline.ui.list.ListMemberPickerItem
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.no_lists
import heron.feature.profile.generated.resources.update_profile_in_lists
import org.jetbrains.compose.resources.stringResource

class ListMemberPickerSheetState(
    scope: BottomSheetScope,
    private val onDismiss: () -> Unit,
) : BottomSheetState(scope) {

    override fun onHidden() {
        onDismiss()
    }

    companion object {
        @Composable
        fun rememberListMemberPickerSheetState(
            listsStateHolder: ProfileScreenStateHolders.Records.Lists?,
            memberships: Map<ListUri, ListMember>,
            profile: Profile,
            onAddListMember: (ProfileId, ListUri) -> Unit,
            onRemoveListMember: (ListMemberUri) -> Unit,
            onShown: () -> Unit,
            onDismiss: () -> Unit,
        ): ListMemberPickerSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = false,
            ) { scope ->
                ListMemberPickerSheetState(
                    scope = scope,
                    onDismiss = onDismiss,
                )
            }
            ListMemberPickerBottomSheet(
                state = state,
                listsStateHolder = listsStateHolder,
                memberships = memberships,
                profile = profile,
                onAddListMember = onAddListMember,
                onRemoveListMember = onRemoveListMember,
                onShown = onShown,
            )
            return state
        }
    }
}

@Composable
fun ListMemberPickerBottomSheet(
    state: ListMemberPickerSheetState,
    profile: Profile,
    listsStateHolder: ProfileScreenStateHolders.Records.Lists?,
    memberships: Map<ListUri, ListMember>,
    onAddListMember: (ProfileId, ListUri) -> Unit,
    onRemoveListMember: (ListMemberUri) -> Unit,
    onShown: () -> Unit,
) {
    state.ModalBottomSheet {
        LaunchedEffect(Unit) {
            onShown()
        }

        Text(
            text = stringResource(
                Res.string.update_profile_in_lists,
                profile.displayName ?: profile.handle.id,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        HorizontalDivider()

        when (listsStateHolder) {
            null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = stringResource(Res.string.no_lists),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            else -> RecordList(
                collectionStateHolder = listsStateHolder,
                prefersCompactBottomNav = false,
                itemKey = { it.cid.id },
                itemContent = { feedList ->
                    ListMemberPickerItem(
                        list = feedList,
                        membership = memberships[feedList.uri],
                        profileId = profile.did,
                        onAddListMember = onAddListMember,
                        onRemoveListMember = onRemoveListMember,
                    )
                },
            )
        }
    }
}
