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

package com.tunjid.heron.models

import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.shouldHideProfileLink
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ProfileLinkModerationTest {

    @Test
    fun unlabeledLinkIsShown() {
        assertFalse(appliedLabels().shouldHideProfileLink())
    }

    @Test
    fun adultSelfLabelHidesLinkWhenAdultContentIsOff() {
        assertTrue(
            appliedLabels(
                label(value = "porn", creatorId = ownerId),
            ).shouldHideProfileLink(),
        )
    }

    @Test
    fun adultLabelIsShownToViewersWhoChoseToSeeIt() {
        assertFalse(
            appliedLabels(
                label(value = "porn", creatorId = ownerId),
                adultContentEnabled = true,
                preferences = listOf(
                    ContentLabelPreference(
                        labelerId = null,
                        label = Label.Value("porn"),
                        visibility = Label.Visibility.Ignore,
                    ),
                ),
            ).shouldHideProfileLink(),
        )
    }

    @Test
    fun adultLabelThatWouldWarnHidesLink() {
        // "sexual" defaults to warn, which a link can't show
        assertTrue(
            appliedLabels(
                label(value = "sexual", creatorId = ownerId),
                adultContentEnabled = true,
            ).shouldHideProfileLink(),
        )
    }

    @Test
    fun globalHideAndWarnLabelsHideLink() {
        assertTrue(appliedLabels(label(value = "!hide", creatorId = labelerId)).shouldHideProfileLink())
        assertTrue(appliedLabels(label(value = "!warn", creatorId = labelerId)).shouldHideProfileLink())
    }

    @Test
    fun labelerWarningThatBlursHidesLink() {
        assertTrue(
            appliedLabels(
                label(value = "scam", creatorId = labelerId),
                labelers = listOf(labeler(blurs = Label.BlurTarget.Content)),
            ).shouldHideProfileLink(),
        )
    }

    @Test
    fun labelerWarningThatDoesNotBlurKeepsLink() {
        assertFalse(
            appliedLabels(
                label(value = "scam", creatorId = labelerId),
                labelers = listOf(labeler(blurs = Label.BlurTarget.None)),
            ).shouldHideProfileLink(),
        )
    }

    @Test
    fun labelFromUnsubscribedLabelerIsIgnored() {
        assertFalse(
            appliedLabels(
                label(value = "scam", creatorId = labelerId),
            ).shouldHideProfileLink(),
        )
    }

    private fun appliedLabels(
        vararg labels: Label,
        adultContentEnabled: Boolean = false,
        labelers: List<Labeler> = emptyList(),
        preferences: List<ContentLabelPreference> = emptyList(),
    ) = AppliedLabels(
        adultContentEnabled = adultContentEnabled,
        labels = labels.toList(),
        labelers = labelers,
        contentLabelPreferences = preferences,
    )

    private fun labeler(
        blurs: Label.BlurTarget,
    ) = Labeler(
        uri = LabelerUri("at://labeler/1"),
        cid = LabelerId("labeler-1"),
        creator = stubProfile(
            did = labelerId,
            handle = ProfileHandle("labeler.test"),
        ),
        likeCount = null,
        definitions = listOf(
            Label.Definition(
                adultOnly = false,
                blurs = blurs,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("scam"),
                severity = Label.Severity.Alert,
            ),
        ),
        values = listOf(Label.Value("scam")),
    )

    private fun label(
        value: String,
        creatorId: ProfileId,
    ) = Label(
        uri = GenericUri("at://${ownerId.id}/app.bsky.actor.link/3kabc"),
        creatorId = creatorId,
        value = Label.Value(value),
        version = 1L,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private companion object {
        val ownerId = ProfileId("did:example:owner")
        val labelerId = ProfileId("did:example:labeler")
    }
}
