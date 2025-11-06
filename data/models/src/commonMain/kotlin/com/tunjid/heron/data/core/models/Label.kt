/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the License);
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an AS IS BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.jvm.JvmInline
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val uri: GenericUri,
    val creatorId: ProfileId,
    val value: Value,
    val version: Long?,
    val createdAt: Instant,
) {
    @JvmInline
    @Serializable
    value class Value(
        val value: String,
    )

    @Serializable
    data class Visibility(
        val value: String,
    ) {
        companion object {
            val Hide = Visibility("hide")
            val Show = Visibility("show")
            val Warn = Visibility("warn")
        }
    }

    @Serializable
    data class Definition(
        val adultOnly: Boolean,
        val blurs: BlurTarget,
        val defaultSetting: Visibility,
        val identifier: Value,
        val severity: Severity,
    )

    enum class BlurTarget {
        Content,
        Media,
        None,
    }

    enum class Severity {
        Alert,
        Inform,
        None,
    }

    companion object {
        val Hidden = Value("!hide")
        val NonAuthenticated = Value("!no-unauthenticated ")
    }
}

@Serializable
data class Labeler(
    val uri: GenericUri,
    val creatorId: ProfileId,
    val definitions: List<Label.Definition>,
    val values: List<Label.Value>,
) {
    @Serializable
    data class LocaleInfo(
        val lang: String,
        val name: String,
        val description: String,
    ) : UrlEncodableModel

    @Serializable
    data class LocalInfoList(
        val list: List<LocaleInfo>,
    ) : UrlEncodableModel
}

typealias Labelers = List<Labeler>

fun labelVisibilitiesToDefinitions(
    postLabels: Set<Label.Value>,
    labelers: Labelers,
    labelsVisibilityMap: Map<Label.Value, Label.Visibility>,
): Map<Label.Visibility, List<Label.Definition>> = when {
    postLabels.isEmpty() -> emptyMap()
    else -> labelers.fold(
        mutableMapOf<Label.Visibility, MutableList<Label.Definition>>(),
    ) { destination, labeler ->
        labeler.definitions.fold(destination) innerFold@{ innerDestination, definition ->
            // Not applicable to this post
            if (!postLabels.contains(definition.identifier)) return@innerFold innerDestination

            val mayBlur = definition.adultOnly ||
                definition.blurs == Label.BlurTarget.Media ||
                definition.blurs == Label.BlurTarget.Content

            if (!mayBlur) return@innerFold innerDestination

            val visibility = labelsVisibilityMap[definition.identifier]
                ?: definition.defaultSetting

            innerDestination.getOrPut(
                key = visibility,
                defaultValue = ::mutableListOf,
            ).add(definition)

            innerDestination
        }
    }
}
