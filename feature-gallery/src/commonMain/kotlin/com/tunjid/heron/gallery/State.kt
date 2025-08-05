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

import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.gallery.di.postId
import com.tunjid.heron.gallery.di.startIndex
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.tunjid.heron.data.core.models.Image as EmbeddedImage
import com.tunjid.heron.data.core.models.Video as EmbeddedVideo


@Serializable
data class State(
    val startIndex: Int,
    val postId: PostId,
    val sharedElementPrefix: String,
    val post: Post?,
    @Transient
    val items: List<GalleryItem> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)

fun State(
    route: Route
) = State(
    startIndex = route.startIndex,
    postId = route.postId,
    post = null,
    sharedElementPrefix = route.sharedElementPrefix,
    items = when (val media = route.model<Embed.Media>()) {
        is ImageList -> media.images.map(GalleryItem::Photo)
        is Video -> listOf(GalleryItem.Video(media))
        null -> emptyList()
    }
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

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(), NavigationAction by delegate
    }
}