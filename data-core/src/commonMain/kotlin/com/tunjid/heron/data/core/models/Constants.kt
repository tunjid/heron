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

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId

object Constants {
    const val UNKNOWN = "at://unknown"

    val timelineFeed = GenericUri("at://self")
    val blockedPostId = PostId("at://blocked")
    val notFoundPostId = PostId("at://not_found")
    val unknownPostId = PostId(UNKNOWN)
    val unknownPostUri = PostUri(UNKNOWN)
    val unknownAuthorId = ProfileId(UNKNOWN)
    val unknownAuthorHandle = ProfileHandle(UNKNOWN)
}

sealed class UriLookup {
    abstract val profileHandleOrDid: Id.Profile

    data class Profile(
        override val profileHandleOrDid: Id.Profile,
    ) : UriLookup()

    data class Post(
        override val profileHandleOrDid: Id.Profile,
        val postUriSuffix: String,
    ) : UriLookup()

    sealed class Timeline : UriLookup() {

        data class Following(
            override val profileHandleOrDid: Id.Profile,
        ) : Timeline()

        data class FeedGenerator(
            override val profileHandleOrDid: Id.Profile,
            val feedUriSuffix: String,
        ) : Timeline()

        data class List(
            override val profileHandleOrDid: Id.Profile,
            val listUriSuffix: String,
        ) : Timeline()

        data class Profile(
            override val profileHandleOrDid: Id.Profile,
            val type: com.tunjid.heron.data.core.models.Timeline.Profile.Type,
        ) : Timeline()
    }

}