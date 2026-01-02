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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId

object Constants {
    private val unknownRecordKey = "2222222222222"
    const val UNKNOWN = "at://unknown"

    val timelineFeed = GenericUri("at://self")

    val blueSkyDiscoverFeed = FeedGeneratorUri("at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot")
    val heronsFeed = FeedGeneratorUri("at://did:plc:coo5y44dzkgujeypdrkjtgk6/app.bsky.feed.generator/herons")
    val blackSkyTrendingFeed = FeedGeneratorUri("at://did:plc:w4xbfzo7kqfes5zb7r6qv3rw/app.bsky.feed.generator/blacksky-trend")

    // Invalid post id format, but its already in the db. Follow up in a migration to replace
    val blockedPostId = PostId("at://blocked")

    // Invalid post id format, but its already in the db. Follow up in a migration to replace
    val notFoundPostId = PostId("at://not_found")
    val unknownPostId = PostId(UNKNOWN)

    // Invalid post uri format, but its already in the db. Follow up in a migration to replace
    val unknownPostUri = PostUri(UNKNOWN)
    val unknownAuthorId = ProfileId(id = "did:web:heron.app.unknown.user")
    val guestProfileId = ProfileId(id = "did:web:heron.app.guest.user")
    val pendingProfileId = ProfileId(id = "did:web:heron.app.pending.user")
    val unknownFollowUri = FollowUri("${unknownAuthorId.id}/${FollowUri.NAMESPACE}/$unknownRecordKey")
    val unknownAuthorHandle = ProfileHandle(UNKNOWN)
}
