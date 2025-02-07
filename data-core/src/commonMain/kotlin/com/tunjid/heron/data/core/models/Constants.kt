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

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri

object Constants {
    const val UNKNOWN = "at://unknown"

    val timelineFeed = Uri("at://self")
    val blockedPostId = Id("at://blocked")
    val notFoundPostId = Id("at://not_found")
    val unknownPostId = Id(UNKNOWN)
    val unknownPostUri = Uri(UNKNOWN)
    val unknownAuthorId = Id(UNKNOWN)
}

sealed class UriLookup {
    abstract val profileHandleOrDid: String

    data class Profile(
        override val profileHandleOrDid: String,
    ) : UriLookup()

    data class Post(
        override val profileHandleOrDid: String,
        val postUriSuffix: String,
    ) : UriLookup()

    sealed class Timeline : UriLookup() {

        data class FeedGenerator(
            override val profileHandleOrDid: String,
            val feedUriSuffix: String,
        ) : Timeline()

        data class List(
            override val profileHandleOrDid: String,
            val listUriSuffix: String,
        ) : Timeline()

        data class Profile(
            override val profileHandleOrDid: String,
            val type: com.tunjid.heron.data.core.models.Timeline.Profile.Type,
        ) : Timeline()
    }

}