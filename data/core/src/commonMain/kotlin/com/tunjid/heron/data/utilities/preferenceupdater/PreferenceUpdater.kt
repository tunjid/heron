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

package com.tunjid.heron.data.utilities.preferenceupdater

import app.bsky.actor.AdultContentPref
import app.bsky.actor.ContentLabelPref
import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.LabelerPrefItem
import app.bsky.actor.LabelersPref
import app.bsky.actor.PreferencesUnion
import app.bsky.actor.SavedFeed
import app.bsky.actor.SavedFeedsPrefV2
import app.bsky.actor.Type
import app.bsky.actor.Visibility
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.utilities.TidGenerator
import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass
import sh.christian.ozone.api.Did

internal interface PreferenceUpdater {
    suspend fun update(
        response: GetPreferencesResponse,
        preferences: Preferences,
    ): Preferences

    suspend fun update(
        response: GetPreferencesResponse,
        update: Timeline.Update,
    ): List<PreferencesUnion>
}

internal class ThingPreferenceUpdater @Inject constructor(
    private val tidGenerator: TidGenerator,
) : PreferenceUpdater {

    override suspend fun update(
        response: GetPreferencesResponse,
        preferences: Preferences,
    ): Preferences = response.preferences.fold(
        // Reset values to be filled from network response
        initial = preferences.copy(
            allowAdultContent = false,
            timelinePreferences = emptyList(),
            contentLabelPreferences = emptyList(),
            labelerPreferences = emptyList(),
        ),
        operation = { foldedPreferences, preferencesUnion ->
            when (preferencesUnion) {
                is PreferencesUnion.AdultContentPref -> foldedPreferences.copy(
                    allowAdultContent = preferencesUnion.value.enabled,
                )
                is PreferencesUnion.BskyAppStatePref -> foldedPreferences
                is PreferencesUnion.ContentLabelPref -> foldedPreferences.copy(
                    contentLabelPreferences = foldedPreferences.contentLabelPreferences
                        .plus(preferencesUnion.asExternalModel()),
                )

                is PreferencesUnion.FeedViewPref -> foldedPreferences
                is PreferencesUnion.HiddenPostsPref -> foldedPreferences
                is PreferencesUnion.InterestsPref -> foldedPreferences
                is PreferencesUnion.LabelersPref -> foldedPreferences.copy(
                    labelerPreferences = preferencesUnion.value.labelers.map {
                        LabelerPreference(
                            labelerCreatorId = it.did.did.let(::ProfileId),
                        )
                    },
                )
                is PreferencesUnion.MutedWordsPref -> foldedPreferences
                is PreferencesUnion.PersonalDetailsPref -> foldedPreferences
                is PreferencesUnion.SavedFeedsPref -> foldedPreferences
                is PreferencesUnion.SavedFeedsPrefV2 -> foldedPreferences.copy(
                    timelinePreferences = preferencesUnion.value.items.map {
                        TimelinePreference(
                            id = it.id,
                            type = it.type.value,
                            value = it.value,
                            pinned = it.pinned,
                        )
                    },
                )

                is PreferencesUnion.ThreadViewPref -> foldedPreferences
                is PreferencesUnion.Unknown -> foldedPreferences
                is PreferencesUnion.PostInteractionSettingsPref -> foldedPreferences
                is PreferencesUnion.VerificationPrefs -> foldedPreferences
            }
        },
    )

    override suspend fun update(
        response: GetPreferencesResponse,
        update: Timeline.Update,
    ): List<PreferencesUnion> {
        val groupedPreferences = response.preferences.groupBy(
            keySelector = { it::class },
        )
        val targetClass = update.targetClass()
        val targetPreferences = groupedPreferences.getOrElse(
            key = targetClass,
            defaultValue = ::emptyList,
        )
        val updatedPreferences = update.updatePreferences(
            existingPreferences = targetPreferences,
        )
        return groupedPreferences
            // Replace the existing value with the updates
            .plus(targetClass to updatedPreferences)
            .flatMap(Map.Entry<KClass<out PreferencesUnion>, List<PreferencesUnion>>::value)
    }

    /**
     * Exclusively operates on the existing saved feed preferences.
     */
    private suspend fun Timeline.Update.HomeFeed.updateHomeTimelinePreferences(
        preferenceUnionList: List<PreferencesUnion.SavedFeedsPrefV2>,
    ): List<PreferencesUnion.SavedFeedsPrefV2> {
        val preferenceUnion = preferenceUnionList.firstOrNull() ?: return emptyList()
        // Return a singleton list
        return listOf(
            PreferencesUnion.SavedFeedsPrefV2(
                SavedFeedsPrefV2(
                    items = when (this) {
                        is Timeline.Update.Bulk -> preferenceUnion.value.items.associateBy(
                            keySelector = SavedFeed::value,
                            valueTransform = SavedFeed::id,
                        ).let { savedFeedValuesToIds ->
                            timelines.mapNotNull { timeline ->
                                when (timeline) {
                                    is Timeline.Home.Feed -> savedFeedValuesToIds[
                                        timeline.feedGenerator.uri.uri,
                                    ]?.let { id ->
                                        SavedFeed(
                                            id = id,
                                            type = Type.Feed,
                                            value = timeline.feedGenerator.uri.uri,
                                            pinned = timeline.isPinned,
                                        )
                                    }

                                    is Timeline.Home.Following -> savedFeedValuesToIds[
                                        "following",
                                    ]?.let { id ->
                                        SavedFeed(
                                            id = id,
                                            type = Type.Timeline,
                                            value = "following",
                                            pinned = timeline.isPinned,
                                        )
                                    }

                                    is Timeline.Home.List -> savedFeedValuesToIds[
                                        timeline.feedList.uri.uri,
                                    ]?.let { id ->
                                        SavedFeed(
                                            id = id,
                                            type = Type.List,
                                            value = timeline.feedList.uri.uri,
                                            pinned = timeline.isPinned,
                                        )
                                    }
                                }
                            }
                        }

                        is Timeline.Update.HomeFeed.Pin -> preferenceUnion.value.items.filter {
                            it.value != uri.uri
                        }
                            .partition(SavedFeed::pinned)
                            .let { (pinned, saved) ->
                                pinned + SavedFeed(
                                    id = tidGenerator.generate(),
                                    type = when (this) {
                                        is Timeline.Update.OfFeedGenerator.Pin -> Type.Feed
                                        is Timeline.Update.OfList.Pin -> Type.List
                                    },
                                    value = uri.uri,
                                    pinned = true,
                                ) + saved
                            }

                        is Timeline.Update.HomeFeed.Remove -> preferenceUnion.value.items.filter { savedFeed ->
                            savedFeed.value != uri.uri
                        }

                        is Timeline.Update.HomeFeed.Save -> preferenceUnion.value.items.filter {
                            it.value != uri.uri
                        } + SavedFeed(
                            id = tidGenerator.generate(),
                            type = when (this) {
                                is Timeline.Update.OfFeedGenerator.Save -> Type.Feed
                                is Timeline.Update.OfList.Save -> Type.List
                            },
                            value = uri.uri,
                            pinned = false,
                        )
                    },
                ),
            ),
        )
    }

    /**
     * A content label preference may or may not already exist for a particular update. This method
     * unconditionally creates the preference, and filters out an existing one if present.
     */
    private fun Timeline.Update.OfContentLabel.updateContentLabelPreferences(
        preferenceUnionList: List<PreferencesUnion.ContentLabelPref>,
    ): List<PreferencesUnion.ContentLabelPref> = when (this) {
        is Timeline.Update.OfContentLabel.AdultLabelVisibilityChange -> label.labelValues.map { label ->
            PreferencesUnion.ContentLabelPref(
                value = ContentLabelPref(
                    labelerDid = null,
                    label = label.value,
                    visibility = Visibility.safeValueOf(visibility.value),
                ),
            )
        }
        is Timeline.Update.OfContentLabel.LabelVisibilityChange -> listOf(
            PreferencesUnion.ContentLabelPref(
                value = ContentLabelPref(
                    labelerDid = labelCreatorId.id.let(::Did),
                    label = value.value,
                    visibility = Visibility.safeValueOf(visibility.value),
                ),
            ),
        )
    }
        .plus(preferenceUnionList)
        .distinctBy { "${it.value.label}-${it.value.labelerDid}" }

    /**
     * A labeler preference may or may not already exist for a particular update.
     * If subscribed, this method unconditionally creates the preference, and filters out an
     * existing one if present.
     * If not subscribed, the existing one is simply removed.
     */
    private fun Timeline.Update.OfLabeler.updateLabelerPreferences(
        preferenceUnionList: List<PreferencesUnion.LabelersPref>,
    ): List<PreferencesUnion.LabelersPref> {
        val labelers = preferenceUnionList
            .flatMap { it.value.labelers }
        // Return a singleton list
        return listOf(
            PreferencesUnion.LabelersPref(
                value = LabelersPref(
                    labelers = when (this) {
                        is Timeline.Update.OfLabeler.Subscription -> when {
                            subscribed ->
                                labelers
                                    .plus(LabelerPrefItem(did = labelCreatorId.id.let(::Did)))
                                    .distinctBy(LabelerPrefItem::did)
                            else ->
                                labelers
                                    .filter { it.did.did != labelCreatorId.id }
                        }
                    },
                ),
            ),
        )
    }

    /**
     * Unconditionally creates the adult content preference and returns it
     */
    private fun Timeline.Update.OfAdultContent.updateAdultContentPreferences(): List<PreferencesUnion.AdultContentPref> =
        // Return a singleton list

        listOf(
            PreferencesUnion.AdultContentPref(
                AdultContentPref(enabled = enabled),
            ),
        )

    private suspend fun Timeline.Update.updatePreferences(
        existingPreferences: List<PreferencesUnion>,
    ): List<PreferencesUnion> =
        when (this) {
            is Timeline.Update.HomeFeed -> updateHomeTimelinePreferences(
                preferenceUnionList = existingPreferences.filterIsInstance<PreferencesUnion.SavedFeedsPrefV2>(),
            )
            is Timeline.Update.OfAdultContent -> updateAdultContentPreferences()
            is Timeline.Update.OfContentLabel -> updateContentLabelPreferences(
                preferenceUnionList = existingPreferences.filterIsInstance<PreferencesUnion.ContentLabelPref>(),
            )
            is Timeline.Update.OfLabeler -> updateLabelerPreferences(
                preferenceUnionList = existingPreferences.filterIsInstance<PreferencesUnion.LabelersPref>(),
            )
        }
}

private fun Timeline.Update.targetClass() =
    when (this) {
        is Timeline.Update.HomeFeed -> PreferencesUnion.SavedFeedsPrefV2::class
        is Timeline.Update.OfAdultContent -> PreferencesUnion.AdultContentPref::class
        is Timeline.Update.OfContentLabel -> PreferencesUnion.ContentLabelPref::class
        is Timeline.Update.OfLabeler -> PreferencesUnion.LabelersPref::class
    }

private fun PreferencesUnion.ContentLabelPref.asExternalModel() = ContentLabelPreference(
    labelerId = value.labelerDid?.did?.let(::ProfileId),
    label = Label.Value(value = value.label),
    visibility = Label.Visibility(value = value.visibility.value),
)
