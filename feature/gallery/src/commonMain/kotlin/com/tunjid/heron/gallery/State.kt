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

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Image as EmbeddedImage
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.Video as EmbeddedVideo
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.gallery.di.postRecordKey
import com.tunjid.heron.gallery.di.profileId
import com.tunjid.heron.gallery.di.startIndex
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.timeline.utilities.MutedWordUpdateAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val startIndex: Int,
    val postUri: PostUri,
    val sharedElementPrefix: String,
    val post: Post?,
    val viewedProfileId: ProfileId,
    val signedInProfileId: ProfileId? = null,
    val viewerState: ProfileViewerState?,
    @Transient
    val preferences: Preferences = Preferences.EmptyPreferences,
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val items: List<GalleryItem> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(
    route: Route,
) = State(
    startIndex = route.startIndex,
    postUri = PostUri(
        route.profileId,
        route.postRecordKey,
    ),
    post = null,
    viewerState = null,
    viewedProfileId = route.profileId,
    sharedElementPrefix = route.sharedElementPrefix,
    items = when (val media = route.model<Embed.Media>()) {
        is ImageList -> media.images.map(GalleryItem::Photo)
        is Video -> listOf(GalleryItem.Video(media))
        null -> emptyList()
    },
)

val State.posterSharedElementPrefix
    get() = "poster-$sharedElementPrefix"

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

    data class UpdateMutedWord(
        override val mutedWordPreference: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord"),
        MutedWordUpdateAction

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: FollowUri?,
        val followedBy: FollowUri?,
    ) : Action(key = "ToggleViewerState")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
