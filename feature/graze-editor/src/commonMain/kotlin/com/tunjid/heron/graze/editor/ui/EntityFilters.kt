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

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.embed_kind_label
import heron.feature.graze_editor.generated.resources.entity_excludes
import heron.feature.graze_editor.generated.resources.entity_matches
import heron.feature.graze_editor.generated.resources.entity_type_domain
import heron.feature.graze_editor.generated.resources.entity_type_hashtag
import heron.feature.graze_editor.generated.resources.entity_type_language
import heron.feature.graze_editor.generated.resources.entity_type_mention
import heron.feature.graze_editor.generated.resources.entity_type_url
import org.jetbrains.compose.resources.stringResource

@Composable
fun EntityFilter(
    filter: Filter.Entity,
    onUpdate: (Filter.Entity) -> Unit,
    onRemove: () -> Unit,
) {
    ChipFilter(
        title = stringResource(
            when (filter) {
                is Filter.Entity.Excludes -> Res.string.entity_excludes
                is Filter.Entity.Matches -> Res.string.entity_matches
            },
        ),
        onRemove = onRemove,
        items = filter.values,
        onItemsUpdated = {
            when (filter) {
                is Filter.Entity.Excludes -> filter.copy(values = it)
                is Filter.Entity.Matches -> filter.copy(values = it)
            }
        },
        startContent = {
            ComparatorDropdown(
                selected = when (filter) {
                    is Filter.Entity.Excludes -> Filter.Comparator.Set.NotIn
                    is Filter.Entity.Matches -> Filter.Comparator.Set.In
                },
                options = Filter.Comparator.Set.entries,
                onSelect = {
                    onUpdate(
                        when (it) {
                            Filter.Comparator.Set.In -> Filter.Entity.Matches(
                                entityType = filter.entityType,
                                values = filter.values,
                            )
                            Filter.Comparator.Set.NotIn -> Filter.Entity.Excludes(
                                entityType = filter.entityType,
                                values = filter.values,
                            )
                        },
                    )
                },
            )
        },
        endContent = {
            Dropdown(
                label = stringResource(Res.string.embed_kind_label),
                selected = filter.entityType,
                options = Filter.Entity.Type.entries,
                stringRes = Filter.Entity.Type::stringRes,
                onSelect = {
                    onUpdate(
                        when (filter) {
                            is Filter.Entity.Excludes -> filter.copy(entityType = it)
                            is Filter.Entity.Matches -> filter.copy(entityType = it)
                        },
                    )
                },
            )
        },
    )
}

private val Filter.Entity.Type.stringRes
    get() = when (this) {
        Filter.Entity.Type.Hashtags -> Res.string.entity_type_hashtag
        Filter.Entity.Type.Languages -> Res.string.entity_type_language
        Filter.Entity.Type.Urls -> Res.string.entity_type_url
        Filter.Entity.Type.Mentions -> Res.string.entity_type_mention
        Filter.Entity.Type.Domains -> Res.string.entity_type_domain
    }
