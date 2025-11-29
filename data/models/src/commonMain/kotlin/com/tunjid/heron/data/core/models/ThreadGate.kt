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

import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ThreadGateUri

class ThreadGate(
    val uri: ThreadGateUri,
    val gatedPostUri: PostUri,
    val allowed: Allowed?,
) {
    data class Allowed(
        val allowsFollowing: Boolean,
        val allowsFollowers: Boolean,
        val allowsMentioned: Boolean,
        val allowedLists: List<FeedList>,
    )
}

val ThreadGate?.allowsFollowing
    get() = this == null || allowed == null || allowed.allowsFollowing

val ThreadGate?.allowsFollowers
    get() = this == null || allowed == null || allowed.allowsFollowers

val ThreadGate?.allowsMentioned
    get() = this == null || allowed == null || allowed.allowsMentioned

val ThreadGate?.allowsLists
    get() = this != null && allowed != null && allowed.allowedLists.isNotEmpty()

val ThreadGate?.allowsAll
    get() = this == null || allowed == null

val ThreadGate?.allowsNone
    get() = !allowsFollowing &&
        !allowsFollowers &&
        !allowsMentioned &&
        !allowsLists
