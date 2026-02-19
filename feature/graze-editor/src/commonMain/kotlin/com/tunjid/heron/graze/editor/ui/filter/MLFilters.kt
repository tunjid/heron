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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.category
import heron.feature.graze_editor.generated.resources.content_moderation
import heron.feature.graze_editor.generated.resources.moderation_category_harassment
import heron.feature.graze_editor.generated.resources.moderation_category_hate
import heron.feature.graze_editor.generated.resources.moderation_category_hate_threatening
import heron.feature.graze_editor.generated.resources.moderation_category_ok
import heron.feature.graze_editor.generated.resources.moderation_category_self_harm
import heron.feature.graze_editor.generated.resources.moderation_category_sexual
import heron.feature.graze_editor.generated.resources.moderation_category_sexual_minors
import heron.feature.graze_editor.generated.resources.moderation_category_unknown
import heron.feature.graze_editor.generated.resources.moderation_category_violence
import heron.feature.graze_editor.generated.resources.moderation_category_violence_graphic
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MLModerationFilter(
    filter: Filter.ML.Moderation,
    onUpdate: (Filter.ML.Moderation) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardFilter(
        modifier = modifier,
        tint = filter.validationTint(),
        title = stringResource(Res.string.content_moderation),
        onRemove = onRemove,
        startContent = {
            Dropdown(
                label = stringResource(Res.string.category),
                selected = filter.category,
                options = Filter.ML.Moderation.Category.entries,
                modifier = Modifier.fillMaxWidth(),
                stringRes = Filter.ML.Moderation.Category::stringRes,
                onSelect = { onUpdate(filter.copy(category = it)) },
            )
        },
        endContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { onUpdate(filter.copy(operator = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        additionalContent = {
            ThresholdSlider(
                threshold = filter.threshold,
                onThresholdChanged = { onUpdate(filter.copy(threshold = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

fun Filter.ML.Moderation.Category.stringRes(): StringResource =
    when (this) {
        Filter.ML.Moderation.Category.Sexual -> Res.string.moderation_category_sexual
        Filter.ML.Moderation.Category.Hate -> Res.string.moderation_category_hate
        Filter.ML.Moderation.Category.Violence -> Res.string.moderation_category_violence
        Filter.ML.Moderation.Category.Harassment -> Res.string.moderation_category_harassment
        Filter.ML.Moderation.Category.SelfHarm -> Res.string.moderation_category_self_harm
        Filter.ML.Moderation.Category.SexualMinors -> Res.string.moderation_category_sexual_minors
        Filter.ML.Moderation.Category.HateThreatening ->
            Res.string.moderation_category_hate_threatening
        Filter.ML.Moderation.Category.ViolenceGraphic ->
            Res.string.moderation_category_violence_graphic
        Filter.ML.Moderation.Category.OK -> Res.string.moderation_category_ok
        else -> Res.string.moderation_category_unknown
    }
