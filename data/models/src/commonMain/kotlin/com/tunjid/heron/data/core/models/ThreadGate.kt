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

import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ThreadGateUri
import kotlinx.serialization.Serializable

class ThreadGate(
    val uri: ThreadGateUri,
    val gatedPostUri: PostUri,
    val allowed: Allowed?,
) {
    @Serializable
    data class Allowed(
        val allowsFollowing: Boolean,
        val allowsFollowers: Boolean,
        val allowsMentioned: Boolean,
        val allowedLists: List<FeedList>,
    )

    @Serializable
    data class Summary(
        val gatedPostUri: PostUri,
        val threadGateUri: ThreadGateUri?,
        val allowsFollowing: Boolean,
        val allowsFollowers: Boolean,
        val allowsMentioned: Boolean,
        val allowedListUris: List<ListUri>,
    )
}

val ThreadGate.Allowed?.allowsFollowing
    get() = this == null || allowsFollowing

val ThreadGate.Allowed?.allowsFollowers
    get() = this == null || allowsFollowers

val ThreadGate.Allowed?.allowsMentioned
    get() = this == null || allowsMentioned

val ThreadGate.Allowed?.allowsLists
    get() = this != null && allowedLists.isNotEmpty()

val ThreadGate.Allowed?.allowsAll
    get() = this == null

val ThreadGate.Allowed?.allowsNone
    get() = !allowsFollowing &&
        !allowsFollowers &&
        !allowsMentioned &&
        !allowsLists
