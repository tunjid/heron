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
sealed class Timeline {

    abstract val sourceId: String
    abstract val name: String

    @Serializable
    sealed class Home(
        val source: Uri,
    ) : Timeline() {

        abstract val position: Int

        override val sourceId: String
            get() = source.uri

        abstract val lastRefreshed: Instant?

        @Serializable
        data class Following(
            override val name: String,
            override val position: Int,
            override val lastRefreshed: Instant?,
        ) : Home(Constants.timelineFeed)

        @Serializable
        data class List(
            override val position: Int,
            override val lastRefreshed: Instant?,
            val feedList: FeedList,
        ) : Home(feedList.uri) {
            override val name: String
                get() = feedList.name
        }

        @Serializable
        data class Feed(
            override val position: Int,
            override val lastRefreshed: Instant?,
            val feedGenerator: FeedGenerator,
        ) : Home(feedGenerator.uri) {
            override val name: String
                get() = feedGenerator.displayName
        }

    }

    @Serializable
    sealed class Profile : Timeline() {

        abstract val profileId: Id

        override val sourceId: String
            get() = when (this) {
                is Media -> "${profileId.id}-media"
                is Posts -> "${profileId.id}-posts"
                is Replies -> "${profileId.id}-posts-and-replies"
            }

        @Serializable
        data class Posts(
            override val name: String,
            override val profileId: Id,
        ) : Profile()

        @Serializable
        data class Replies(
            override val name: String,
            override val profileId: Id,
        ) : Profile()

        @Serializable
        data class Media(
            override val name: String,
            override val profileId: Id,
        ) : Profile()

    }
}

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Thread,
            is Single,
                -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Thread(
        override val id: String,
        val anchorPostIndex: Int,
        val posts: List<Post>,
        val generation: Long?,
        val hasBreak: Boolean,
    ) : TimelineItem() {
        override val post: Post
            get() = posts[anchorPostIndex]
    }

    data class Single(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()
}
