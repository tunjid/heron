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

package com.tunjid.heron.graze.editor.ui.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.graze.editor.ui.SelectTextSheetState.Companion.rememberSelectTextState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.attribute_compare
import heron.feature.graze_editor.generated.resources.edit_item
import heron.feature.graze_editor.generated.resources.embed_kind_gif
import heron.feature.graze_editor.generated.resources.embed_kind_image
import heron.feature.graze_editor.generated.resources.embed_kind_image_group
import heron.feature.graze_editor.generated.resources.embed_kind_label
import heron.feature.graze_editor.generated.resources.embed_kind_link
import heron.feature.graze_editor.generated.resources.embed_kind_post
import heron.feature.graze_editor.generated.resources.embed_kind_video
import heron.feature.graze_editor.generated.resources.embed_type
import heron.feature.graze_editor.generated.resources.selector
import heron.feature.graze_editor.generated.resources.selector_embed
import heron.feature.graze_editor.generated.resources.selector_mention_handle
import heron.feature.graze_editor.generated.resources.selector_quote_author_handle
import heron.feature.graze_editor.generated.resources.selector_reply
import heron.feature.graze_editor.generated.resources.selector_text
import heron.feature.graze_editor.generated.resources.selector_unknown
import heron.feature.graze_editor.generated.resources.selector_user_handle
import heron.feature.graze_editor.generated.resources.value
import org.jetbrains.compose.resources.StringResource
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
        tint = filter.validationTint(),
        title = stringResource(Res.string.attribute_compare),
        onRemove = onRemove,
        startContent = {
            Dropdown(
                label = stringResource(Res.string.selector),
                selected = filter.selector,
                options = Filter.Attribute.Compare.Selector.entries,
                stringRes = Filter.Attribute.Compare.Selector::stringRes,
                onSelect = { onUpdate(filter.copy(selector = it)) },
            )
        },
        endContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Equality.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
            )
        },
        additionalContent = {
            val selectTextSheetState = rememberSelectTextState(
                title = stringResource(Res.string.attribute_compare),
                onTextConfirmed = {
                    onUpdate(filter.copy(targetValue = it))
                },
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = filter.targetValue,
                readOnly = true,
                onValueChange = { },
                label = { Text(text = stringResource(Res.string.value)) },
            )
            FilledTonalButton(
                onClick = {
                    selectTextSheetState.show(currentText = filter.targetValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(text = stringResource(Res.string.edit_item))
            }
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
        tint = filter.validationTint(),
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

private fun Filter.Attribute.Compare.Selector.stringRes(): StringResource = when (this) {
    Filter.Attribute.Compare.Selector.Text -> Res.string.selector_text
    Filter.Attribute.Compare.Selector.Reply -> Res.string.selector_reply
    Filter.Attribute.Compare.Selector.Embed -> Res.string.selector_embed
    Filter.Attribute.Compare.Selector.UserHandle -> Res.string.selector_user_handle
    Filter.Attribute.Compare.Selector.MentionHandle -> Res.string.selector_mention_handle
    Filter.Attribute.Compare.Selector.QuoteAuthorHandle -> Res.string.selector_quote_author_handle
    else -> Res.string.selector_unknown
}
