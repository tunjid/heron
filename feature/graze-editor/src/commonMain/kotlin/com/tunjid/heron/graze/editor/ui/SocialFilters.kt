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

package com.tunjid.heron.graze.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.graze.editor.ui.EditFilterTextSheetState.Companion.rememberEditFilterProfileTextState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_item
import heron.feature.graze_editor.generated.resources.audience_id
import heron.feature.graze_editor.generated.resources.direction
import heron.feature.graze_editor.generated.resources.edit_item
import heron.feature.graze_editor.generated.resources.social_graph
import heron.feature.graze_editor.generated.resources.social_graph_direction_followers
import heron.feature.graze_editor.generated.resources.social_graph_direction_following
import heron.feature.graze_editor.generated.resources.social_graph_direction_unknown
import heron.feature.graze_editor.generated.resources.social_list_member
import heron.feature.graze_editor.generated.resources.social_magic_audience
import heron.feature.graze_editor.generated.resources.social_starter_pack
import heron.feature.graze_editor.generated.resources.social_user_list
import heron.feature.graze_editor.generated.resources.url
import heron.feature.graze_editor.generated.resources.username
import org.jetbrains.compose.resources.stringResource

@Composable
fun SocialGraphFilter(
    filter: Filter.Social.Graph,
    results: List<Profile>,
    onProfileQueryChanged: (String) -> Unit,
    onUpdate: (Filter.Social.Graph) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        title = stringResource(Res.string.social_graph),
        onRemove = onRemove,
        startContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Set.entries,
                onSelect = { comparator ->
                    onUpdate(
                        filter.copy(operator = comparator),
                    )
                },
            )
        },
        endContent = {
            Dropdown(
                label = stringResource(Res.string.direction),
                selected = filter.direction,
                options = Filter.Social.Graph.Direction.entries,
                stringRes = Filter.Social.Graph.Direction::stringRes,
                onSelect = { direction ->
                    onUpdate(
                        filter.copy(
                            direction = direction,
                        ),
                    )
                },
            )
        },
        additionalContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val addTextSheetState = rememberEditFilterProfileTextState(
                    title = stringResource(Res.string.username),
                    suggestedProfiles = results,
                    onTextConfirmed = { profileHandle ->
                        onUpdate(
                            filter.copy(
                                username = profileHandle,
                            ),
                        )
                    },
                )
                OutlinedTextField(
                    value = filter.username,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(text = stringResource(Res.string.username))
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                FilledTonalButton(
                    onClick = {
                        addTextSheetState.show(
                            currentText = filter.username,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (filter.username.isBlank()) Res.string.add_item
                            else Res.string.edit_item,
                        ),
                    )
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { addTextSheetState.options.text }
                        .collect(onProfileQueryChanged)
                }
            }
        },
    )
}

@Composable
fun SocialUserListFilter(
    filter: Filter.Social.UserList,
    results: List<Profile>,
    onProfileQueryChanged: (String) -> Unit,
    onUpdate: (Filter.Social.UserList) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        title = stringResource(Res.string.social_user_list),
        onRemove = onRemove,
        rowContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Set.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        },

        additionalContent = {
            val onItemsUpdated: (List<String>) -> Unit = {
                onUpdate(
                    filter.copy(dids = it),
                )
            }
            val editFilterTextSheetState = rememberEditFilterProfileTextState(
                title = stringResource(Res.string.username),
                suggestedProfiles = results,
                onItemsUpdated = onItemsUpdated,
                items = filter.dids,
            )
            FilterTextChips(
                editFilterTextSheetState = editFilterTextSheetState,
                onItemsUpdated = onItemsUpdated,
                items = filter.dids,
            )
            LaunchedEffect(Unit) {
                snapshotFlow { editFilterTextSheetState.options.text }
                    .collect(onProfileQueryChanged)
            }
        },
    )
}

@Composable
fun SocialStarterPackFilter(
    filter: Filter.Social.StarterPack,
    onUpdate: (Filter.Social.StarterPack) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterCard(
        modifier = modifier,
        onRemove = onRemove,
    ) {
        Text(
            text = stringResource(Res.string.social_starter_pack),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = filter.url,
            onValueChange = { onUpdate(filter.copy(url = it)) },
            label = { Text(text = stringResource(Res.string.url)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SocialListMemberFilter(
    filter: Filter.Social.ListMember,
    onUpdate: (Filter.Social.ListMember) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterCard(
        modifier = modifier,
        onRemove = onRemove,
    ) {
        Text(
            text = stringResource(Res.string.social_list_member),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = filter.url,
            onValueChange = { onUpdate(filter.copy(url = it)) },
            label = { Text(text = stringResource(Res.string.url)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SocialMagicAudienceFilter(
    filter: Filter.Social.MagicAudience,
    onUpdate: (Filter.Social.MagicAudience) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterCard(
        modifier = modifier,
        onRemove = onRemove,
    ) {
        Text(
            text = stringResource(Res.string.social_magic_audience),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        OutlinedTextField(
            value = filter.audienceId,
            onValueChange = { onUpdate(filter.copy(audienceId = it)) },
            label = { Text(text = stringResource(Res.string.audience_id)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val Filter.Social.Graph.Direction.stringRes
    get() = when (this) {
        Filter.Social.Graph.Direction.Following -> Res.string.social_graph_direction_following
        Filter.Social.Graph.Direction.Followers -> Res.string.social_graph_direction_followers
        else -> Res.string.social_graph_direction_unknown
    }
