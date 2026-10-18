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
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.fakes.samplePost
import com.tunjid.heron.fakes.sampleProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TimelineItemFeedContextTest {

    private val appliedLabels = AppliedLabels(
        adultContentEnabled = false,
        labels = emptyList(),
        labelers = emptyList(),
        preferenceLabelsVisibilityMap = emptyMap(),
    )

    @Test
    fun feedBearingItem_exposesContextThroughParent() {
        val item: TimelineItem = TimelineItem.Single(
            id = "1",
            post = samplePost(),
            isMuted = false,
            threadGate = null,
            appliedLabels = appliedLabels,
            signedInProfileId = null,
            feedContext = "ctx-abc",
            reqId = FeedReqId("req-123"),
        )

        assertEquals(
            expected = "ctx-abc",
            actual = item.feedContext,
        )
        assertEquals(
            expected = FeedReqId("req-123"),
            actual = item.reqId,
        )
    }

    @Test
    fun repostedReply_keepsReasonAndContextOnThread() {
        val repostedAt = Instant.fromEpochSeconds(1_000)
        val reposter = sampleProfile()
        val post = samplePost()
        val node = TimelineItem.Threaded.Node(
            post = post,
            threadGate = null,
            appliedLabels = appliedLabels,
            isMuted = false,
        )
        val item: TimelineItem = TimelineItem.Threaded.Linear(
            id = "1",
            signedInProfileId = null,
            anchorPostIndex = 2,
            nodes = listOf(
                node,
                node,
                node,
            ),
            generation = null,
            hasBreak = false,
            reason = TimelineItem.Reason.Repost(
                by = reposter,
                at = repostedAt,
            ),
            feedContext = "ctx-abc",
            reqId = FeedReqId("req-123"),
        )

        assertEquals(
            expected = reposter,
            actual = (item.reason as TimelineItem.Reason.Repost).by,
        )
        assertEquals(
            expected = repostedAt,
            actual = item.indexedAt,
        )
        assertEquals(
            expected = "ctx-abc",
            actual = item.feedContext,
        )
    }

    @Test
    fun placeholderItem_hasNoContext() {
        val item: TimelineItem = TimelineItem.Loading()

        assertNull(item.reason)
        assertNull(item.feedContext)
        assertNull(item.reqId)
    }
}
