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

package com.tunjid.heron.gallery

import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.tunjid.heron.data.core.models.Image as EmbeddedImage
import com.tunjid.heron.data.core.models.Video as EmbeddedVideo


@Serializable
data class State(
    val startIndex: Int,
    val postId: PostId,
    val sharedElementPrefix: String,
    @Transient
    val items: List<GalleryItem> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)

sealed class GalleryItem {
    data class Photo(
        val image: EmbeddedImage,
    ) : GalleryItem()

    data class Video(
        val video: EmbeddedVideo,
    ) : GalleryItem()
}

val GalleryItem.key
    get() = when (this) {
        is GalleryItem.Photo -> image.thumb.uri
        is GalleryItem.Video -> video.playlist.uri
    }

sealed class Action(val key: String) {

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}