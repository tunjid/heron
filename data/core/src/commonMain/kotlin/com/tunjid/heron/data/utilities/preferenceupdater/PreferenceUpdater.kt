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

import app.bsky.actor.ContentLabelPref
import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.LabelerPrefItem
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
        val (contentLabelPrefs, otherPrefs) = response.preferences
            .partition { it is PreferencesUnion.ContentLabelPref }

        val existingContentLabelPrefs = contentLabelPrefs
            .filterIsInstance<PreferencesUnion.ContentLabelPref>()

        val updatedOtherPrefs = otherPrefs.map { preferencesUnion ->
            when (preferencesUnion) {
                is PreferencesUnion.AdultContentPref -> preferencesUnion.targeting<Timeline.Update.OfAdultContent>(
                    update = update,
                    block = { updateAdultContentPreference(preferencesUnion, it) },
                )

                is PreferencesUnion.LabelersPref -> preferencesUnion.targeting<Timeline.Update.OfLabeler>(
                    update = update,
                    block = { updateLabelerPreference(preferencesUnion, it) },
                )

                is PreferencesUnion.SavedFeedsPrefV2 -> preferencesUnion.targeting<Timeline.Update.HomeFeed>(
                    update = update,
                    block = { updateHomeTimelinePreference(preferencesUnion, it) },
                )

                else -> preferencesUnion
            }
        }

        val updatedContentLabelPrefs =
            if (update is Timeline.Update.OfContentLabel) update.contentLabelPrefs()
                .plus(existingContentLabelPrefs)
                .distinctBy { "${it.value.label}-${it.value.labelerDid}" }
            else existingContentLabelPrefs

        return updatedOtherPrefs + updatedContentLabelPrefs
    }

    private suspend fun updateHomeTimelinePreference(
        preferenceUnion: PreferencesUnion.SavedFeedsPrefV2,
        update: Timeline.Update.HomeFeed,
    ): PreferencesUnion.SavedFeedsPrefV2 = PreferencesUnion.SavedFeedsPrefV2(
        SavedFeedsPrefV2(
            items = when (update) {
                is Timeline.Update.Bulk -> preferenceUnion.value.items.associateBy(
                    keySelector = SavedFeed::value,
                    valueTransform = SavedFeed::id,
                ).let { savedFeedValuesToIds ->
                    update.timelines.mapNotNull { timeline ->
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
                    it.value != update.uri.uri
                }
                    .partition(SavedFeed::pinned)
                    .let { (pinned, saved) ->
                        pinned + SavedFeed(
                            id = tidGenerator.generate(),
                            type = when (update) {
                                is Timeline.Update.OfFeedGenerator.Pin -> Type.Feed
                                is Timeline.Update.OfList.Pin -> Type.List
                            },
                            value = update.uri.uri,
                            pinned = true,
                        ) + saved
                    }

                is Timeline.Update.HomeFeed.Remove -> preferenceUnion.value.items.filter { savedFeed ->
                    savedFeed.value != update.uri.uri
                }

                is Timeline.Update.HomeFeed.Save -> preferenceUnion.value.items.filter {
                    it.value != update.uri.uri
                } + SavedFeed(
                    id = tidGenerator.generate(),
                    type = when (update) {
                        is Timeline.Update.OfFeedGenerator.Save -> Type.Feed
                        is Timeline.Update.OfList.Save -> Type.List
                    },
                    value = update.uri.uri,
                    pinned = false,
                )
            },
        ),
    )

    private fun Timeline.Update.OfContentLabel.contentLabelPrefs() = when (this) {
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

    private fun updateLabelerPreference(
        preferenceUnion: PreferencesUnion.LabelersPref,
        update: Timeline.Update.OfLabeler,
    ): PreferencesUnion.LabelersPref = PreferencesUnion.LabelersPref(
        value = preferenceUnion.value.copy(
            labelers = when (update) {
                is Timeline.Update.OfLabeler.Subscription -> when {
                    update.subscribed ->
                        preferenceUnion.value.labelers
                            .plus(LabelerPrefItem(did = update.labelCreatorId.id.let(::Did)))
                            .distinctBy(LabelerPrefItem::did)
                    else ->
                        preferenceUnion.value.labelers
                            .filter { it.did.did != update.labelCreatorId.id }
                }
            },
        ),
    )

    private fun updateAdultContentPreference(
        preferenceUnion: PreferencesUnion.AdultContentPref,
        update: Timeline.Update.OfAdultContent,
    ): PreferencesUnion.AdultContentPref = PreferencesUnion.AdultContentPref(
        value = preferenceUnion.value.copy(
            enabled = update.enabled,
        ),
    )
}

private fun PreferencesUnion.ContentLabelPref.asExternalModel() = ContentLabelPreference(
    labelerId = value.labelerDid?.did?.let(::ProfileId),
    label = Label.Value(value = value.label),
    visibility = Label.Visibility(value = value.visibility.value),
)

private inline fun <
    reified T : Timeline.Update,
    > PreferencesUnion.targeting(
    update: Timeline.Update,
    block: (T) -> PreferencesUnion,
): PreferencesUnion =
    if (update is T) block(update)
    else this
