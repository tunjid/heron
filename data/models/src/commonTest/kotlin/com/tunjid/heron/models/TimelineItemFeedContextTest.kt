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
import com.tunjid.heron.data.core.models.feedContext
import com.tunjid.heron.data.core.models.reqId
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.fakes.samplePost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimelineItemFeedContextTest {

    private val appliedLabels = AppliedLabels(
        adultContentEnabled = false,
        labels = emptyList(),
        labelers = emptyList(),
        preferenceLabelsVisibilityMap = emptyMap(),
    )

    @Test
    fun feedBearingItem_exposesContextThroughParentExtension() {
        // Typed as the parent so resolution goes through the extension, not the member.
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
    fun placeholderItem_hasNoContext() {
        val item: TimelineItem = TimelineItem.Loading()

        assertNull(item.feedContext)
        assertNull(item.reqId)
    }
}
