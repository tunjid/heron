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

import app.bsky.actor.GetPreferencesResponse
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

internal interface PreferenceUpdater {
    suspend fun update(
        response: GetPreferencesResponse,
        preferences: Preferences,
    ): Preferences

    suspend fun update(
        preferencesUnion: PreferencesUnion,
        update: Timeline.Update,
    ): PreferencesUnion
}

internal class ThingPreferenceUpdater @Inject constructor(
    private val tidGenerator: TidGenerator,
) : PreferenceUpdater {

    override suspend fun update(
        response: GetPreferencesResponse,
        preferences: Preferences,
    ): Preferences = response.preferences.fold(
        initial = preferences,
        operation = { foldedPreferences, preferencesUnion ->
            when (preferencesUnion) {
                is PreferencesUnion.AdultContentPref -> foldedPreferences
                is PreferencesUnion.BskyAppStatePref -> foldedPreferences
                is PreferencesUnion.ContentLabelPref -> foldedPreferences.copy(
                    contentLabelPreferences = preferencesUnion.asExternalModel().let { newPref ->
                        foldedPreferences.contentLabelPreferences
                            .filterNot {
                                it.label == newPref.label && it.labelerId == newPref.labelerId
                            }
                            .plus(newPref)
                    },
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
        preferencesUnion: PreferencesUnion,
        update: Timeline.Update,
    ) = when (preferencesUnion) {
        is PreferencesUnion.AdultContentPref -> preferencesUnion
        is PreferencesUnion.BskyAppStatePref -> preferencesUnion
        is PreferencesUnion.ContentLabelPref -> preferencesUnion.targeting<Timeline.Update.OfContentLabel>(
            update = update,
            block = { updateContentLabelPreference(preferencesUnion, it) },
        )
        is PreferencesUnion.FeedViewPref -> preferencesUnion
        is PreferencesUnion.HiddenPostsPref -> preferencesUnion
        is PreferencesUnion.InterestsPref -> preferencesUnion
        is PreferencesUnion.LabelersPref -> preferencesUnion
        is PreferencesUnion.MutedWordsPref -> preferencesUnion
        is PreferencesUnion.PersonalDetailsPref -> preferencesUnion
        is PreferencesUnion.PostInteractionSettingsPref -> preferencesUnion
        is PreferencesUnion.SavedFeedsPref -> preferencesUnion
        is PreferencesUnion.SavedFeedsPrefV2 -> preferencesUnion.targeting<Timeline.Update.OfFeedGenerator>(
            update = update,
            block = { updateFeedPreference(preferencesUnion, it) },
        )
        is PreferencesUnion.ThreadViewPref -> preferencesUnion
        is PreferencesUnion.Unknown -> preferencesUnion
        is PreferencesUnion.VerificationPrefs -> preferencesUnion
    }

    private suspend fun updateFeedPreference(
        preferenceUnion: PreferencesUnion.SavedFeedsPrefV2,
        update: Timeline.Update.OfFeedGenerator,
    ): PreferencesUnion.SavedFeedsPrefV2 = PreferencesUnion.SavedFeedsPrefV2(
        SavedFeedsPrefV2(
            items = when (update) {
                is Timeline.Update.OfFeedGenerator.Bulk -> preferenceUnion.value.items.associateBy(
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

                is Timeline.Update.OfFeedGenerator.Pin -> preferenceUnion.value.items.filter {
                    it.value != update.uri.uri
                }
                    .partition(SavedFeed::pinned)
                    .let { (pinned, saved) ->
                        pinned + SavedFeed(
                            id = tidGenerator.generate(),
                            type = Type.Feed,
                            value = update.uri.uri,
                            pinned = true,
                        ) + saved
                    }

                is Timeline.Update.OfFeedGenerator.Remove -> preferenceUnion.value.items.filter { savedFeed ->
                    if (savedFeed.type != Type.Feed) return@filter true
                    savedFeed.value != update.uri.uri
                }

                is Timeline.Update.OfFeedGenerator.Save -> preferenceUnion.value.items.filter {
                    it.value != update.uri.uri
                } + SavedFeed(
                    id = tidGenerator.generate(),
                    type = Type.Feed,
                    value = update.uri.uri,
                    pinned = false,
                )
            },
        ),
    )

    private fun updateContentLabelPreference(
        preferenceUnion: PreferencesUnion.ContentLabelPref,
        update: Timeline.Update.OfContentLabel,
    ): PreferencesUnion.ContentLabelPref = when (update) {
        is Timeline.Update.OfContentLabel.VisibilityChange -> when {
            preferenceUnion.value.label != update.value.value -> preferenceUnion
            preferenceUnion.value.labelerDid?.did != update.labelCreatorId.id -> preferenceUnion
            else -> PreferencesUnion.ContentLabelPref(
                value = preferenceUnion.value.copy(visibility = Visibility.safeValueOf(update.visibility.value)),
            )
        }
    }
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
