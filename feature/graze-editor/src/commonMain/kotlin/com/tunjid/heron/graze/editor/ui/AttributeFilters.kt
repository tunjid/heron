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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.attribute_compare
import heron.feature.graze_editor.generated.resources.embed_kind_gif
import heron.feature.graze_editor.generated.resources.embed_kind_image
import heron.feature.graze_editor.generated.resources.embed_kind_image_group
import heron.feature.graze_editor.generated.resources.embed_kind_label
import heron.feature.graze_editor.generated.resources.embed_kind_link
import heron.feature.graze_editor.generated.resources.embed_kind_post
import heron.feature.graze_editor.generated.resources.embed_kind_video
import heron.feature.graze_editor.generated.resources.embed_type
import heron.feature.graze_editor.generated.resources.selector
import heron.feature.graze_editor.generated.resources.value
import org.jetbrains.compose.resources.stringResource

@Composable
fun AttributeCompareFilter(
    filter: Filter.Attribute.Compare,
    onUpdate: (Filter.Attribute.Compare) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        title = stringResource(Res.string.attribute_compare),
        onRemove = onRemove,
        startContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
            )
        },
        endContent = {
            OutlinedTextField(
                value = filter.selector,
                onValueChange = { onUpdate(filter.copy(selector = it)) },
                label = { Text(text = stringResource(Res.string.selector)) },
            )
        },
        additionalContent = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = filter.targetValue,
                onValueChange = { onUpdate(filter.copy(targetValue = it)) },
                label = {
                    Text(text = stringResource(Res.string.value))
                },
            )
        },
    )
}

@Composable
fun AttributeEmbedFilter(
    filter: Filter.Attribute.Embed,
    onUpdate: (Filter.Attribute.Embed) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        title = stringResource(Res.string.embed_type),
        onRemove = onRemove,
        startContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
            )
        },
        endContent = {
            Dropdown(
                label = stringResource(Res.string.embed_kind_label),
                selected = filter.embedType,
                options = Filter.Attribute.Embed.Kind.entries,
                stringRes = Filter.Attribute.Embed.Kind::stringRes,
                onSelect = { onUpdate(filter.copy(embedType = it)) },
            )
        },
    )
}

private fun Filter.Attribute.Embed.Kind.stringRes() = when (this) {
    Filter.Attribute.Embed.Kind.Image -> Res.string.embed_kind_image
    Filter.Attribute.Embed.Kind.Link -> Res.string.embed_kind_link
    Filter.Attribute.Embed.Kind.Post -> Res.string.embed_kind_post
    Filter.Attribute.Embed.Kind.ImageGroup -> Res.string.embed_kind_image_group
    Filter.Attribute.Embed.Kind.Video -> Res.string.embed_kind_video
    Filter.Attribute.Embed.Kind.Gif -> Res.string.embed_kind_gif
}
