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
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class AppliedLabelsSeverityTest {

    @Test
    fun severityNoneMediaLabelStillBlursMedia() {
        val labels = adultContentAppliedLabels(severity = Label.Severity.None)

        assertTrue(labels.shouldBlurMedia)
        assertEquals(Label.Severity.None, labels.blurredMediaSeverity)
        assertFalse(labels.canAutoPlayVideo)
    }

    @Test
    fun mediaLabelPreservesAlertSeverity() {
        val labels = adultContentAppliedLabels(severity = Label.Severity.Alert)

        assertTrue(labels.shouldBlurMedia)
        assertEquals(Label.Severity.Alert, labels.blurredMediaSeverity)
        assertFalse(labels.canAutoPlayVideo)
    }

    @Test
    fun mediaLabelPreservesInformSeverity() {
        val labels = adultContentAppliedLabels(severity = Label.Severity.Inform)

        assertTrue(labels.shouldBlurMedia)
        assertEquals(Label.Severity.Inform, labels.blurredMediaSeverity)
        assertFalse(labels.canAutoPlayVideo)
    }

    @Test
    fun emptyLabelsDoNotBlurMedia() {
        val labels = AppliedLabels.Empty

        assertFalse(labels.shouldBlurMedia)
        assertTrue(labels.canAutoPlayVideo)
    }

    private fun adultContentAppliedLabels(
        severity: Label.Severity,
    ) = AppliedLabels(
        adultContentEnabled = true,
        labels = listOf(label),
        labelers = listOf(labeler(severity)),
        preferenceLabelsVisibilityMap = emptyMap(),
    )

    private fun labeler(
        severity: Label.Severity,
    ) = Labeler(
        uri = LabelerUri("at://labeler/1"),
        cid = LabelerId("labeler-1"),
        creator = stubProfile(
            did = labelerProfileId,
            handle = ProfileHandle("labeler.test"),
        ),
        likeCount = null,
        definitions = listOf(
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Media,
                defaultSetting = Label.Visibility.Warn,
                identifier = labelValue,
                severity = severity,
            ),
        ),
        values = listOf(labelValue),
    )

    private companion object {
        val labelerProfileId = ProfileId("did:example:labeler")
        val labelValue = Label.Value("sexual-figurative")
        val label = Label(
            uri = GenericUri("at://did:example:author/app.bsky.feed.post/3kabc"),
            creatorId = labelerProfileId,
            value = labelValue,
            version = 1L,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
    }
}
