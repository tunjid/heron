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
import app.bsky.actor.ContentLabelPrefVisibility
import app.bsky.actor.LabelerPrefItem
import app.bsky.actor.LabelersPref
import app.bsky.actor.MutedWord
import app.bsky.actor.MutedWordActorTarget
import app.bsky.actor.MutedWordTarget
import app.bsky.actor.MutedWordsPref
import app.bsky.actor.PostInteractionSettingsPref
import app.bsky.actor.PostInteractionSettingsPrefPostgateEmbeddingRuleUnion as BskyPostEmbedGateRule
import app.bsky.actor.PostInteractionSettingsPrefThreadgateAllowRuleUnion as BskyPostReplyGateRule
import app.bsky.actor.PreferencesUnion
import app.bsky.actor.SavedFeed
import app.bsky.actor.SavedFeedType
import app.bsky.actor.SavedFeedsPrefV2
import app.bsky.feed.PostgateDisableRule
import app.bsky.feed.ThreadgateFollowerRule
import app.bsky.feed.ThreadgateFollowingRule
import app.bsky.feed.ThreadgateListRule
import app.bsky.feed.ThreadgateMentionRule
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.DeclaredAgePreference
import com.tunjid.heron.data.core.models.HiddenPostPreference
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference.Companion.embedsDisabled
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.ThreadViewPreference
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.VerificationPreference
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.utilities.TidGenerator
import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

internal interface PreferenceUpdater {
    suspend fun update(
        networkPreferences: List<PreferencesUnion>,
        preferences: Preferences,
    ): Preferences

    suspend fun update(
        networkPreferences: List<PreferencesUnion>,
        update: Timeline.Update,
    ): List<PreferencesUnion>
}

internal class ThingPreferenceUpdater @Inject constructor(
    private val tidGenerator: TidGenerator,
) : PreferenceUpdater {

    override suspend fun update(
        networkPreferences: List<PreferencesUnion>,
        preferences: Preferences,
    ): Preferences = networkPreferences.fold(
        // Reset values to be filled from network response
        initial = preferences.copy(
            allowAdultContent = false,
            timelinePreferences = emptyList(),
            contentLabelPreferences = emptyList(),
            labelerPreferences = emptyList(),
            hiddenPostPreferences = emptyList(),
            mutedWordPreferences = emptyList(),
            declaredAgePreferences = null,
            threadViewPreferences = null,
            postInteractionSettings = null,
            verificationPreferences = null,
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
                is PreferencesUnion.HiddenPostsPref -> foldedPreferences.copy(
                    hiddenPostPreferences = preferencesUnion.value.items.map {
                        HiddenPostPreference(
                            uri = it.atUri.let(::PostUri),
                        )
                    },
                )
                is PreferencesUnion.InterestsPref -> foldedPreferences
                is PreferencesUnion.LabelersPref -> foldedPreferences.copy(
                    labelerPreferences = preferencesUnion.value.labelers.map {
                        LabelerPreference(
                            labelerCreatorId = it.did.did.let(::ProfileId),
                        )
                    },
                )
                is PreferencesUnion.MutedWordsPref -> foldedPreferences.copy(
                    mutedWordPreferences = preferencesUnion.value.items.map {
                        MutedWordPreference(
                            value = it.value,
                            targets = it.targets.map { target ->
                                MutedWordPreference.Target(target.value)
                            },
                            actorTarget = it.actorTarget
                                ?.value
                                ?.let(MutedWordPreference::Target),
                            expiresAt = it.expiresAt,
                        )
                    },
                )
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

                is PreferencesUnion.ThreadViewPref -> foldedPreferences.copy(
                    threadViewPreferences = ThreadViewPreference(
                        sort = preferencesUnion.value.sort?.value,
                    ),
                )
                is PreferencesUnion.Unknown -> foldedPreferences
                is PreferencesUnion.PostInteractionSettingsPref -> foldedPreferences.copy(
                    postInteractionSettings = PostInteractionSettingsPreference(
                        threadGateAllowed = when (
                            val allow =
                                preferencesUnion.value.threadgateAllowRules
                        ) {
                            // All can reply
                            null -> null
                            // None can reply
                            emptyList<BskyPostReplyGateRule>() -> ThreadGate.Allowed(
                                allowsFollowing = false,
                                allowsFollowers = false,
                                allowsMentioned = false,
                            )
                            else ->
                                allow
                                    .groupBy { it::class }
                                    .let { grouped ->
                                        // ThreadgateAllowUnion.ListRule is handled with the list views above
                                        ThreadGate.Allowed(
                                            allowsFollowing = BskyPostReplyGateRule.FollowingRule::class in grouped,
                                            allowsFollowers = BskyPostReplyGateRule.FollowerRule::class in grouped,
                                            allowsMentioned = BskyPostReplyGateRule.MentionRule::class in grouped,
                                            allowedListUris = grouped[BskyPostReplyGateRule.ListRule::class]
                                                .orEmpty()
                                                .mapNotNull {
                                                    if (it is BskyPostReplyGateRule.ListRule) ListUri(
                                                        it.value.list.atUri,
                                                    )
                                                    else null
                                                },
                                        )
                                    }
                        },
                        allowedEmbeds = preferencesUnion.value.postgateEmbeddingRules?.let {
                            it.fold(
                                initial = PostInteractionSettingsPreference.AllowedEmbeds(),
                                operation = { allowed, value ->
                                    when (value) {
                                        is BskyPostEmbedGateRule.DisableRule ->
                                            allowed.copy(none = true)
                                        is BskyPostEmbedGateRule.Unknown ->
                                            allowed
                                    }
                                },
                            )
                        },
                    ),
                )
                is PreferencesUnion.VerificationPrefs -> foldedPreferences.copy(
                    verificationPreferences = VerificationPreference(
                        hideBadges = preferencesUnion.value.hideBadges ?: false,
                    ),
                )
                is PreferencesUnion.DeclaredAgePref -> foldedPreferences.copy(
                    declaredAgePreferences = DeclaredAgePreference(
                        minAge = when {
                            preferencesUnion.value.isOverAge18 == true -> 18
                            preferencesUnion.value.isOverAge16 == true -> 16
                            preferencesUnion.value.isOverAge13 == true -> 13
                            else -> null
                        },
                    ),
                )
            }
        },
    )

    override suspend fun update(
        networkPreferences: List<PreferencesUnion>,
        update: Timeline.Update,
    ): List<PreferencesUnion> {
        val groupedPreferences = networkPreferences.groupBy(
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
        existingPreferences: List<PreferencesUnion.SavedFeedsPrefV2>,
    ): List<PreferencesUnion.SavedFeedsPrefV2> {
        // Do not edit preferences if none exist. The baseline is always defined serverside.
        val preferenceUnion = existingPreferences.firstOrNull() ?: return emptyList()
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
                                            type = SavedFeedType.Feed,
                                            value = timeline.feedGenerator.uri.uri,
                                            pinned = timeline.isPinned,
                                        )
                                    }

                                    is Timeline.Home.Following -> savedFeedValuesToIds[
                                        "following",
                                    ]?.let { id ->
                                        SavedFeed(
                                            id = id,
                                            type = SavedFeedType.Timeline,
                                            value = "following",
                                            pinned = timeline.isPinned,
                                        )
                                    }

                                    is Timeline.Home.List -> savedFeedValuesToIds[
                                        timeline.feedList.uri.uri,
                                    ]?.let { id ->
                                        SavedFeed(
                                            id = id,
                                            type = SavedFeedType.List,
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
                                        is Timeline.Update.OfFeedGenerator.Pin -> SavedFeedType.Feed
                                        is Timeline.Update.OfList.Pin -> SavedFeedType.List
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
                                is Timeline.Update.OfFeedGenerator.Save -> SavedFeedType.Feed
                                is Timeline.Update.OfList.Save -> SavedFeedType.List
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
        existingPreferences: List<PreferencesUnion.ContentLabelPref>,
    ): List<PreferencesUnion.ContentLabelPref> = when (this) {
        is Timeline.Update.OfContentLabel.AdultLabelVisibilityChange -> label.labelValues.map { label ->
            PreferencesUnion.ContentLabelPref(
                value = ContentLabelPref(
                    labelerDid = null,
                    label = label.value,
                    visibility = ContentLabelPrefVisibility.safeValueOf(visibility.value),
                ),
            )
        }
        is Timeline.Update.OfContentLabel.LabelVisibilityChange -> listOf(
            PreferencesUnion.ContentLabelPref(
                value = ContentLabelPref(
                    labelerDid = labelCreatorId.id.let(::Did),
                    label = value.value,
                    visibility = ContentLabelPrefVisibility.safeValueOf(visibility.value),
                ),
            ),
        )
    }
        .plus(existingPreferences)
        .distinctBy { "${it.value.label}-${it.value.labelerDid}" }

    /**
     * A labeler preference may or may not already exist for a particular update.
     * If subscribed, this method unconditionally creates the preference, and filters out an
     * existing one if present.
     * If not subscribed, the existing one is simply removed.
     */
    private fun Timeline.Update.OfLabeler.updateLabelerPreferences(
        existingPreferences: List<PreferencesUnion.LabelersPref>,
    ): List<PreferencesUnion.LabelersPref> {
        val labelers = existingPreferences
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

    private fun Timeline.Update.OfMutedWord.updateMutedWordPreferences(): List<PreferencesUnion.MutedWordsPref> =
        when (this) {
            is Timeline.Update.OfMutedWord.ReplaceAll -> listOf(
                PreferencesUnion.MutedWordsPref(
                    value = MutedWordsPref(
                        items = mutedWordPreferences.map { pref ->
                            MutedWord(
                                id = null,
                                value = pref.value,
                                targets = pref.targets.map {
                                    MutedWordTarget.safeValueOf(it.value)
                                },
                                actorTarget = pref.actorTarget?.let {
                                    MutedWordActorTarget.safeValueOf(it.value)
                                },
                                expiresAt = pref.expiresAt,
                            )
                        },
                    ),
                ),
            )
        }

    private fun Timeline.Update.OfInteractionSettings.updatePostInteractionPreferences(
        existingPreference: PreferencesUnion.PostInteractionSettingsPref?,
    ): List<PreferencesUnion.PostInteractionSettingsPref> {
        val threadGateAllowRules = preference.threadGateAllowed?.let {
            buildList {
                if (it.allowsFollowing) add(
                    BskyPostReplyGateRule.FollowingRule(ThreadgateFollowingRule),
                )
                if (it.allowsFollowers) add(
                    BskyPostReplyGateRule.FollowerRule(ThreadgateFollowerRule),
                )
                if (it.allowsMentioned) add(
                    BskyPostReplyGateRule.MentionRule(ThreadgateMentionRule),
                )
                it.allowedListUris
                    .map { BskyPostReplyGateRule.ListRule(ThreadgateListRule(it.uri.let(::AtUri))) }
                    .let(::addAll)
            }
        }
        val postgateEmbeddingRules = if (preference.allowedEmbeds.embedsDisabled) {
            listOf(BskyPostEmbedGateRule.DisableRule(PostgateDisableRule))
        } else {
            null
        }
        return listOf(
            PreferencesUnion.PostInteractionSettingsPref(
                value = when (existingPreference) {
                    null -> PostInteractionSettingsPref(
                        threadgateAllowRules = threadGateAllowRules,
                        postgateEmbeddingRules = postgateEmbeddingRules,
                    )

                    else -> existingPreference.value.copy(
                        threadgateAllowRules = threadGateAllowRules,
                        postgateEmbeddingRules = postgateEmbeddingRules,
                    )
                },
            ),
        )
    }

    private suspend fun Timeline.Update.updatePreferences(
        existingPreferences: List<PreferencesUnion>,
    ): List<PreferencesUnion> =
        when (this) {
            is Timeline.Update.HomeFeed -> updateHomeTimelinePreferences(
                existingPreferences = existingPreferences.filterIsInstance<PreferencesUnion.SavedFeedsPrefV2>(),
            )
            is Timeline.Update.OfAdultContent -> updateAdultContentPreferences()
            is Timeline.Update.OfContentLabel -> updateContentLabelPreferences(
                existingPreferences = existingPreferences.filterIsInstance<PreferencesUnion.ContentLabelPref>(),
            )
            is Timeline.Update.OfLabeler -> updateLabelerPreferences(
                existingPreferences = existingPreferences.filterIsInstance<PreferencesUnion.LabelersPref>(),
            )
            is Timeline.Update.OfMutedWord -> updateMutedWordPreferences()
            // Only one of these should exist
            is Timeline.Update.OfInteractionSettings -> updatePostInteractionPreferences(
                existingPreference = existingPreferences.filterIsInstance<PreferencesUnion.PostInteractionSettingsPref>()
                    .firstOrNull(),
            )
        }
}

private fun Timeline.Update.targetClass() =
    when (this) {
        is Timeline.Update.HomeFeed -> PreferencesUnion.SavedFeedsPrefV2::class
        is Timeline.Update.OfAdultContent -> PreferencesUnion.AdultContentPref::class
        is Timeline.Update.OfContentLabel -> PreferencesUnion.ContentLabelPref::class
        is Timeline.Update.OfLabeler -> PreferencesUnion.LabelersPref::class
        is Timeline.Update.OfMutedWord -> PreferencesUnion.MutedWordsPref::class
        is Timeline.Update.OfInteractionSettings -> PreferencesUnion.PostInteractionSettingsPref::class
    }

private fun PreferencesUnion.ContentLabelPref.asExternalModel() = ContentLabelPreference(
    labelerId = value.labelerDid?.did?.let(::ProfileId),
    label = Label.Value(value = value.label),
    visibility = Label.Visibility(value = value.visibility.value),
)
