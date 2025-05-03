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
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
sealed class Notification {

    sealed class PostAssociated : Notification() {
        abstract val associatedPost: Post
    }

    abstract val uri: Uri
    abstract val cid: Id
    abstract val author: Profile
    abstract val reasonSubject: Uri?
    abstract val isRead: Boolean
    abstract val indexedAt: Instant

    data class Liked(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        override val associatedPost: Post,
    ) : PostAssociated()

    data class Reposted(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        override val associatedPost: Post,
    ) : PostAssociated()

    data class Followed(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Mentioned(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        override val associatedPost: Post,
    ) : PostAssociated()

    data class RepliedTo(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        override val associatedPost: Post,
    ) : PostAssociated()

    data class Quoted(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        override val associatedPost: Post,
    ) : PostAssociated()

    data class JoinedStarterPack(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Unknown(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Verified(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Unverified(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()


    enum class Reason {
        Unknown,
        Like,
        Repost,
        Follow,
        Mention,
        Reply,
        Quote,
        JoinedStarterPack,
        Verified,
        Unverified,
    }
}

val Notification.associatedPostUri
    get() = when (this) {
        is Notification.Followed -> null
        is Notification.JoinedStarterPack -> null
        is Notification.Liked -> associatedPost.uri
        is Notification.Mentioned -> null
        is Notification.Quoted -> null
        is Notification.RepliedTo -> null
        is Notification.Reposted -> associatedPost.uri
        is Notification.Unknown -> null
        is Notification.Unverified -> null
        is Notification.Verified -> null
    }