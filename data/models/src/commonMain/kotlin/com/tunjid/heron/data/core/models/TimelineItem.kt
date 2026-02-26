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

import com.tunjid.heron.data.core.models.Timeline.Source
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val Source.id
    get() = when (this) {
        Source.Following -> Constants.timelineFeed.uri
        is Source.Profile -> type.sourceId(profileId)
        is Source.Record.Feed -> uri.uri
        is Source.Record.List -> uri.uri
    }

val Timeline.sourceId: String
    get() = source.id

val Timeline.Home.uri: Uri
    get() = when (val source = source) {
        Source.Following -> Constants.timelineFeed
        is Source.Record.Feed -> source.uri
        is Source.Record.List -> source.uri
    }

val Timeline.uri: Uri?
    get() = when (this) {
        is Timeline.Home -> uri
        is Timeline.Profile -> null
        is Timeline.StarterPack -> listTimeline.uri
    }

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post
    abstract val isMuted: Boolean
    abstract val threadGate: ThreadGate?
    abstract val appliedLabels: AppliedLabels
    abstract val signedInProfileId: ProfileId?

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Thread,
            is Single,
            is Placeholder,
            is ReplyTree,
            -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Thread(
        override val id: String,
        override val isMuted: Boolean,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
        val anchorPostIndex: Int,
        val posts: List<Post>,
        val postUrisToThreadGates: Map<PostUri, ThreadGate?>,
        val generation: Long?,
        val hasBreak: Boolean,
    ) : TimelineItem() {
        override val post: Post
            get() = posts[anchorPostIndex]
        override val threadGate: ThreadGate?
            get() = postUrisToThreadGates[post.uri]
    }

    data class Single(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
    ) : TimelineItem()

    data class ReplyTree(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
        val replies: List<ReplyNode>,
    ) : TimelineItem()
    sealed class Placeholder : TimelineItem() {
        override val post: Post
            get() = LoadingPost

        override val isMuted: Boolean = false
        override val threadGate: ThreadGate? = null
        override val appliedLabels: AppliedLabels = LoadingAppliedLabels
        override val signedInProfileId: ProfileId? = null
    }

    data class Loading @OptIn(ExperimentalUuidApi::class) constructor(
        override val id: String = Uuid.random().toString(),
    ) : Placeholder()

    data class Empty(
        val timeline: Timeline,
    ) : Placeholder() {
        override val id: String = timeline.sourceId
    }

    companion object {

        private val LoadingPost = Post(
            cid = Constants.blockedPostId,
            uri = Constants.unknownPostUri,
            author = stubProfile(
                did = Constants.unknownAuthorId,
                handle = Constants.unknownAuthorHandle,
            ),
            replyCount = 0,
            repostCount = 0,
            likeCount = 0,
            quoteCount = 0,
            bookmarkCount = 0,
            indexedAt = Instant.DISTANT_PAST,
            embed = null,
            record = null,
            viewerStats = null,
            labels = emptyList(),
            embeddedRecord = null,
            viewerState = null,
        )

        private val LoadingAppliedLabels = AppliedLabels(
            adultContentEnabled = false,
            labels = emptyList(),
            labelers = emptyList(),
            preferenceLabelsVisibilityMap = emptyMap(),
        )

        val LoadingItems = (0..16).map { Loading() }
    }
}

data class ReplyNode(
    val post: Post,
    val threadGate: ThreadGate?,
    val appliedLabels: AppliedLabels,
    val depth: Int,
    val children: List<ReplyNode>,
)
