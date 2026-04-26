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
import com.tunjid.heron.data.core.types.ListUri
import kotlinx.serialization.Serializable

@Serializable
sealed class ProfileTab : UrlEncodableModel {

    @Serializable
    sealed class Bluesky : ProfileTab() {
        @Serializable
        sealed class Posts : Bluesky() {
            @Serializable
            data object Standard : Posts()

            @Serializable
            data object Replies : Posts()

            @Serializable
            data object Likes : Posts()

            @Serializable
            data object Media : Posts()

            @Serializable
            data object Videos : Posts()
        }

        @Serializable
        sealed class Lists : Bluesky() {
            @Serializable
            data object All : Lists()
        }

        @Serializable
        sealed class FeedGenerators : Bluesky() {

            @Serializable
            data object All : FeedGenerators()

            @Serializable
            data class FeedGenerator(
                val uri: FeedGeneratorUri,
            ) : FeedGenerators()
        }

        @Serializable
        data object StarterPacks : Bluesky()
    }

    @Serializable
    sealed class StandardSite : ProfileTab() {
        @Serializable
        data object Publications : StandardSite()

        @Serializable
        data object Documents : StandardSite()
    }
}

val ProfileTab.Bluesky.Posts.profileTimelineType: Timeline.Profile.Type
    get() = when (this) {
        ProfileTab.Bluesky.Posts.Standard -> Timeline.Profile.Type.Posts
        ProfileTab.Bluesky.Posts.Replies -> Timeline.Profile.Type.Replies
        ProfileTab.Bluesky.Posts.Likes -> Timeline.Profile.Type.Likes
        ProfileTab.Bluesky.Posts.Media -> Timeline.Profile.Type.Media
        ProfileTab.Bluesky.Posts.Videos -> Timeline.Profile.Type.Videos
    }
