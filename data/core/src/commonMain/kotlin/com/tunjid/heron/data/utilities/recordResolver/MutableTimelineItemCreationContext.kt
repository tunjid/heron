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

package com.tunjid.heron.data.utilities.recordResolver

import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.isMuted
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PopulatedThreadGateEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import kotlin.time.Clock

internal class MutableTimelineItemCreationContext(
    override val signedInProfileId: ProfileId?,
    private val preferences: Preferences,
    associatedRecords: List<Record.Embeddable>,
    associatedThreadGateEntities: List<PopulatedThreadGateEntity>,
    associatedProfileEntities: List<PopulatedProfileEntity>,
) : RecordResolver.TimelineItemCreationContext,
    MutableList<TimelineItem> by mutableListOf() {

    override var list: MutableList<TimelineItem> = this

    override val post: Post
        get() = requireNotNull(currentPost)

    override var appliedLabels: AppliedLabels = AppliedLabels(
        adultContentEnabled = false,
        labels = emptyList(),
        labelers = emptyList(),
        contentLabelPreferences = emptyList(),
    )

    private var currentPost: Post? = null

    private val recordUrisToRecords =
        if (associatedRecords.isEmpty()) emptyMap()
        else associatedRecords.associateBy {
            it.reference.uri
        }

    private val postUrisToThreadGateEntities =
        if (associatedThreadGateEntities.isEmpty()) emptyMap()
        else associatedThreadGateEntities.associateBy(
            keySelector = { it.entity.gatedPostUri },
            valueTransform = PopulatedThreadGateEntity::asExternalModel,
        )

    private val profileIdsToProfiles =
        if (associatedProfileEntities.isEmpty()) emptyMap()
        else associatedProfileEntities.associateBy(
            keySelector = { it.entity.did },
            valueTransform = PopulatedProfileEntity::asExternalModel,
        )

    override fun record(recordUri: EmbeddableRecordUri): Record? =
        recordUrisToRecords[recordUri]

    override fun threadGate(postUri: PostUri): ThreadGate? =
        postUrisToThreadGateEntities[postUri]

    override fun profile(profileId: ProfileId): Profile? =
        profileIdsToProfiles[profileId]

    override fun isMuted(
        post: Post,
    ): Boolean = with(post) {
        if (viewerState.isMuted) return true

        val record = this.record ?: return false
        if (preferences.mutedWordPreferences.isEmpty()) return false

        val now = Clock.System.now()
        val text = record.text

        for (preference in preferences.mutedWordPreferences) {
            val expiresAt = preference.expiresAt
            if (expiresAt != null && expiresAt <= now) {
                continue
            }

            for (target in preference.targets) {
                when (target) {
                    MutedWordPreference.ContentTarget -> {
                        if (text.containsWholeWord(preference.value)) {
                            return true
                        }
                    }
                    MutedWordPreference.TagTarget -> {
                        val normalizedMuteWord = preference.value.removePrefix("#")
                        for (link in record.links) {
                            val linkTarget = link.target
                            if (linkTarget is LinkTarget.Hashtag) {
                                if (linkTarget.tag.equals(normalizedMuteWord, ignoreCase = true)) {
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }
        return when (val embedded = embeddedRecord) {
            is Post -> isMuted(embedded)
            else -> false
        }
    }

    /**
     * Checks if [substring] appears in this CharSequence as a whole word (surrounded by boundaries).
     * This mimics Regex pattern "\b$substring\b" but does 0 allocations.
     */
    private fun CharSequence.containsWholeWord(substring: String): Boolean {
        if (substring.isEmpty()) return false
        var startIndex = 0

        while (true) {
            val index = this.indexOf(substring, startIndex, ignoreCase = true)
            if (index == -1) return false

            // Check boundary before
            val startBoundary = if (index == 0) true else !this[index - 1].isWordCharacter()

            // Check boundary after
            val end = index + substring.length
            val endBoundary = if (end == this.length) true else !this[end].isWordCharacter()

            if (startBoundary && endBoundary) {
                return true
            }

            startIndex = index + 1
        }
    }

    // Helper to define what part of a word is (mimics Regex \w: [a-zA-Z0-9_])
    private fun Char.isWordCharacter(): Boolean {
        return this.isLetterOrDigit() || this == '_'
    }

    fun update(
        currentPost: Post,
        appliedLabels: AppliedLabels,
    ) {
        this.currentPost = currentPost
        this.appliedLabels = appliedLabels
    }
}
