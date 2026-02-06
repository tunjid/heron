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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter

@Composable
fun SocialGraphFilter(
    filter: Filter.Social.Graph,
    onUpdate: (Filter.Social.Graph) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Social Graph", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.username,
            onValueChange = { onUpdate(filter.copy(username = it)) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Set.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.direction,
                onValueChange = { onUpdate(filter.copy(direction = it)) },
                label = { Text("Direction") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SocialUserListFilter(
    filter: Filter.Social.UserList,
    onUpdate: (Filter.Social.UserList) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Social User List", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.dids.joinToString(", "),
            onValueChange = { onUpdate(filter.copy(dids = it.split(",").map { s -> s.trim() })) },
            label = { Text("DIDs (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SocialStarterPackFilter(
    filter: Filter.Social.StarterPack,
    onUpdate: (Filter.Social.StarterPack) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Social Starter Pack", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.url,
            onValueChange = { onUpdate(filter.copy(url = it)) },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SocialListMemberFilter(
    filter: Filter.Social.ListMember,
    onUpdate: (Filter.Social.ListMember) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Social List Member", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.url,
            onValueChange = { onUpdate(filter.copy(url = it)) },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SocialMagicAudienceFilter(
    filter: Filter.Social.MagicAudience,
    onUpdate: (Filter.Social.MagicAudience) -> Unit,
    onRemove: () -> Unit,
) {
    FilterCard(onRemove = onRemove) {
        Text("Social Magic Audience", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ComparatorDropdown(
            selected = filter.operator,
            options = Filter.Comparator.Set.entries,
            onSelect = { onUpdate(filter.copy(operator = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter.audienceId,
            onValueChange = { onUpdate(filter.copy(audienceId = it)) },
            label = { Text("Audience ID") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
