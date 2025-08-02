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

package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labelers
import com.tunjid.heron.data.core.models.Post

val Post.createdAt get() = record?.createdAt ?: indexedAt

fun Post.blurredMediaLabels(
    labelers: Labelers,
    contentPreferences: ContentLabelPreferences,
): List<Label.Value> {
    if (labels.isEmpty()) return emptyList()

    val contentPreferenceMap = contentPreferences.associateBy(
        keySelector = ContentLabelPreference::label,
        valueTransform = { it.visibility }
    )

    val mediaBlurLabels = labelers.flatMapTo(mutableSetOf()) { labeler ->
        labeler.definitions.mapNotNull { definition ->
            when {
                definition.adultOnly -> definition.identifier
                definition.blurs == Label.BlurTarget.Media -> definition.identifier
                else -> null
            }
        }
    }
    val postBlurredLabels = labels.mapNotNull { label ->
        if (mediaBlurLabels.contains(label.value)) when (
            val visibility = contentPreferenceMap[label.value]
        ) {
            null -> label.value
            else -> label.value.takeIf { visibility.shouldBlurMedia }
        }
        else null
    }

    return postBlurredLabels
}

val Label.Visibility.shouldBlurMedia
    get() = when (this) {
        Label.Visibility.Hide -> true
        Label.Visibility.Warn -> true
        else -> false
    }
