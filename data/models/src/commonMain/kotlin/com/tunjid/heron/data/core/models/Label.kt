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

import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import kotlin.jvm.JvmInline
import kotlin.time.Instant
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

    enum class Adult(
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
        val NonAuthenticated = Value("!no-unauthenticated")

        internal val AdultLabels = Adult.entries.flatMapTo(mutableSetOf(), Adult::labelValues)
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
) : Record,
    Record.Embeddable {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    override val embeddableRecordUri: EmbeddableRecordUri
        get() = uri

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
sealed interface AppliedLabels {
    val labels: Collection<Label>
    val shouldHide: Boolean
    val shouldBlurMedia: Boolean
    val blurredMediaSeverity: Label.Severity
    val canAutoPlayVideo: Boolean

    fun visibility(label: Label.Value): Label.Visibility
    fun definition(label: Label): Label.Definition?
    fun labelerSummary(label: Label): LabelerSummary?

    data class LabelerSummary(
        val creatorId: ProfileId,
        val creatorHandle: ProfileHandle,
        val creatorAvatar: ImageUri?,
    )

    companion object {
        operator fun invoke(
            adultContentEnabled: Boolean,
            labels: Collection<Label>,
            labelers: Labelers,
            preferenceLabelsVisibilityMap: Map<Label.Value, Label.Visibility>,
        ): AppliedLabels = AppliedLabelsImpl(
            adultContentEnabled = adultContentEnabled,
            labels = labels,
            labelers = labelers,
            preferenceLabelsVisibilityMap = preferenceLabelsVisibilityMap,
        )

        operator fun invoke(
            adultContentEnabled: Boolean,
            labels: Collection<Label>,
            labelers: Labelers,
            contentLabelPreferences: ContentLabelPreferences,
        ): AppliedLabels = AppliedLabelsImpl(
            adultContentEnabled = adultContentEnabled,
            labels = labels,
            labelers = labelers,
            preferenceLabelsVisibilityMap = contentLabelPreferences.associateBy(
                keySelector = ContentLabelPreference::label,
                valueTransform = ContentLabelPreference::visibility,
            ),
        )
    }
}

private data class AppliedLabelsImpl(
    private val adultContentEnabled: Boolean,
    override val labels: Collection<Label>,
    private val labelers: Labelers,
    private val preferenceLabelsVisibilityMap: Map<Label.Value, Label.Visibility>,
) : AppliedLabels {

    private val postLabels =
        if (labels.isEmpty()) emptySet()
        else labels.mapNotNullTo(mutableSetOf()) {
            val isPostUri = it.uri.uri.asRecordUriOrNull() is PostUri
            if (isPostUri) it.value else null
        }

    override val shouldHide: Boolean =
        postLabels.any { labelValue ->
            if (labelValue == Label.Hidden) return@any true
            visibility(labelValue) == Label.Visibility.Hide
        }

    override val shouldBlurMedia: Boolean =
        postLabels.any { labelValue ->
            if (labelValue == Label.Warn) return@any true

            val isBlurTarget = Label.AdultLabels.contains(labelValue) ||
                findDefinition(labelValue)?.blurs == Label.BlurTarget.Media

            isBlurTarget && visibility(labelValue) == Label.Visibility.Warn
        }

    override val blurredMediaSeverity: Label.Severity =
        postLabels.firstNotNullOfOrNull { labelValue ->
            findDefinition(labelValue)
                ?.takeIf { it.blurs == Label.BlurTarget.Media || it.adultOnly }
                ?.severity
        } ?: Label.Severity.Inform

    override val canAutoPlayVideo: Boolean = !shouldBlurMedia

    override fun visibility(label: Label.Value): Label.Visibility {
        // Check in custom labelers first
        val definition = findDefinition(label)
        if (definition != null) {
            if (definition.adultOnly && !adultContentEnabled) return Label.Visibility.Hide
            return preferenceLabelsVisibilityMap[label] ?: definition.defaultSetting
        }
        // Next check known adult labelers
        for (adultLabel in Label.Adult.entries) {
            if (label in adultLabel.labelValues) {
                if (!adultContentEnabled) return Label.Visibility.Hide
                return preferenceLabelsVisibilityMap[label] ?: adultLabel.defaultVisibility
            }
        }
        return Label.Visibility.Ignore
    }

    override fun definition(label: Label): Label.Definition? =
        labelers.find { it.creator.did == label.creatorId }
            ?.definitions
            ?.find { it.identifier == label.value }

    override fun labelerSummary(label: Label): AppliedLabels.LabelerSummary? =
        labelers.find { it.creator.did == label.creatorId }?.let { labeler ->
            AppliedLabels.LabelerSummary(
                creatorId = labeler.creator.did,
                creatorHandle = labeler.creator.handle,
                creatorAvatar = labeler.creator.avatar,
            )
        }

    private fun findDefinition(label: Label.Value): Label.Definition? {
        for (i in labelers.lastIndex downTo 0) {
            val labeler = labelers[i]
            val def = labeler.definitions.find { it.identifier == label }
            if (def != null) return def
        }
        return null
    }
}
