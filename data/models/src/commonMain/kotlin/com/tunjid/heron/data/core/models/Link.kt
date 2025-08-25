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
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val start: Int,
    val end: Int,
    val target: LinkTarget,
)

@Serializable
sealed interface LinkTarget {
    @Serializable
    data class UserHandleMention(
        val handle: ProfileHandle,
    ) : OfProfile, Navigable

    @Serializable
    data class UserDidMention(
        val did: ProfileId,
    ) : OfProfile, Navigable

    sealed interface OfProfile: LinkTarget

    sealed interface Navigable: LinkTarget

    @Serializable
    data class ExternalLink(
        val uri: GenericUri,
    ) : LinkTarget

    @Serializable
    data class Hashtag(
        val tag: String,
    ) : LinkTarget, Navigable
}

val LinkTarget.Navigable.path: String
    get() = when(this) {
        is LinkTarget.UserDidMention -> "/profile/${did.id}"
        is LinkTarget.UserHandleMention -> "/profile/${handle.id}"
        is LinkTarget.Hashtag -> "/search/#$tag"
    }