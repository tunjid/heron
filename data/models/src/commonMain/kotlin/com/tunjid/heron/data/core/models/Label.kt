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
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.asRecordUriOrNull
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
            val Warn = Visibility("warn") // Also show badge
            val Ignore = Visibility("ignore")

            val all = listOf(
                Ignore,
                Warn,
                Hide,
            )
        }
    }

    @Serializable
    data class Definition(
        val adultOnly: Boolean,
        val blurs: BlurTarget,
        val defaultSetting: Visibility,
        val identifier: Value,
        val severity: Severity,
        val locales: Labeler.LocaleInfoList = Labeler.LocaleInfoList(emptyList()),
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

    enum class Global(
        val defaultVisibility: Visibility,
        val labelValues: List<Value>,
    ) {
        AdultContent(
            defaultVisibility = Visibility.Hide,
            labelValues = listOf(
                Value("porn"),
                Value("nsfw"),
            ),
        ),
        SexuallySuggestive(
            defaultVisibility = Visibility.Warn,
            labelValues = listOf(
                Value("sexual"),
                Value("suggestive"),
            ),
        ),
        GraphicMedia(
            defaultVisibility = Visibility.Warn,
            labelValues = listOf(
                Value("graphic-media"),
                Value("gore"),
            ),
        ),
        NonSexualNudity(
            defaultVisibility = Visibility.Ignore,
            labelValues = listOf(
                Value("nudity"),
            ),
        ),
    }

    companion object {
        val Hidden = Value("!hide")
        val Warn = Value("!warn")

        val NonAuthenticated = Value("!no-unauthenticated ")

        internal val AdultLabels = Global.entries.flatMapTo(mutableSetOf(), Global::labelValues)
    }
}

@Serializable
data class Labeler(
    val uri: LabelerUri,
    val cid: LabelerId,
    val creator: Profile,
    val likeCount: Long?,
    val definitions: List<Label.Definition>,
    val values: List<Label.Value>,
) : Record {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    @Serializable
    data class LocaleInfo(
        val lang: String,
        val name: String,
        val description: String,
    ) : UrlEncodableModel

    @Serializable
    data class LocaleInfoList(
        val list: List<LocaleInfo>,
    ) : UrlEncodableModel
}

typealias Labelers = List<Labeler>

/**
 * A class holding metadata about a profile's preferences for labels.
 */
data class AppliedLabels(
    val adultContentEnabled: Boolean,
    val labels: Collection<Label>,
    val labelers: Labelers,
    val preferenceLabelsVisibilityMap: Map<Label.Value, Label.Visibility>,
) {
    constructor(
        adultContentEnabled: Boolean,
        labels: Collection<Label>,
        labelers: Labelers,
        contentLabelPreferences: ContentLabelPreferences,
    ) : this(
        adultContentEnabled = adultContentEnabled,
        labels = labels,
        labelers = labelers,
        preferenceLabelsVisibilityMap = contentLabelPreferences.associateBy(
            keySelector = ContentLabelPreference::label,
            valueTransform = ContentLabelPreference::visibility,
        ),
    )

    private val postLabels =
        if (labels.isEmpty()) emptySet()
        else labels.mapNotNullTo(mutableSetOf()) {
            val isPostUri = it.uri.uri.asRecordUriOrNull() is PostUri
            if (isPostUri) it.value else null
        }

    private val labelValuesToDefinitions = labelers.flatMap(Labeler::definitions)
        .associateBy(
            keySelector = Label.Definition::identifier,
        )

    private val labelVisibilityMap: Map<Label.Value, Label.Visibility> by lazy {
        Label.Global.entries.fold(
            initial = mutableMapOf<Label.Value, Label.Visibility>(),
            operation = { result, globalLabel ->
                globalLabel.labelValues.forEach { labelValue ->
                    val isAdultLabel = Label.AdultLabels.contains(labelValue)
                    result[labelValue] =
                        if (isAdultLabel && !adultContentEnabled) Label.Visibility.Hide
                        else preferenceLabelsVisibilityMap.getOrElse(
                            labelValue,
                            globalLabel::defaultVisibility,
                        )
                }
                result
            },
        ) +
            labelers.flatMap(Labeler::definitions)
                .associateBy(
                    keySelector = Label.Definition::identifier,
                    valueTransform = { definition ->
                        if (definition.adultOnly && !adultContentEnabled) Label.Visibility.Hide
                        else preferenceLabelsVisibilityMap.getOrElse(
                            definition.identifier,
                            definition::defaultSetting,
                        )
                    },
                )
    }

    fun visibility(label: Label.Value) =
        labelVisibilityMap[label] ?: Label.Visibility.Ignore

    val shouldHide: Boolean
        get() = postLabels.any { labelValue ->
            if (labelValue == Label.Hidden) return@any true
            visibility(labelValue) == Label.Visibility.Hide
        }

    val shouldBlurMedia: Boolean
        get() = postLabels.any { labelValue ->
            if (labelValue == Label.Warn) return@any true

            val isBlurTarget = Label.AdultLabels.contains(labelValue) ||
                labelValuesToDefinitions[labelValue]?.blurs == Label.BlurTarget.Media

            isBlurTarget && visibility(labelValue) == Label.Visibility.Warn
        }

    val blurredMediaSeverity: Label.Severity
        get() = postLabels.firstNotNullOfOrNull { labelValue ->
            labelValuesToDefinitions[labelValue]
                ?.takeIf { it.blurs == Label.BlurTarget.Media || it.adultOnly }
                ?.severity
        } ?: Label.Severity.Inform

    val canAutoPlayVideo: Boolean
        get() = !shouldBlurMedia
}
