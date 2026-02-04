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

import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Image as EmbeddedImage
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.Video as EmbeddedVideo
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.gallery.di.postRecordKey
import com.tunjid.heron.gallery.di.profileId
import com.tunjid.heron.gallery.di.startIndex
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.tiledListOf
import com.tunjid.treenav.strings.Route
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val sharedElementPrefix: String,
    val viewedProfileId: ProfileId,
    val signedInProfileId: ProfileId? = null,
    val canScrollVertically: Boolean = false,
    val cursorData: CursorQuery.Data?,
    @Transient
    val preferences: Preferences = Preferences.EmptyPreferences,
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val items: TiledList<CursorQuery, GalleryItem> = emptyTiledList(),
    @Transient
    val timelineStateHolder: TimelineStateHolder? = null,
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(
    route: Route,
) = State(
    viewedProfileId = route.profileId,
    sharedElementPrefix = route.sharedElementPrefix,
    cursorData = route.model<CursorQuery.Data>(),
    items = tiledListOf(
        DataQuery(
            data = route.model<CursorQuery.Data>() ?: CursorQuery.defaultStartData(),
        ) to GalleryItem.Initial(
            sharedElementPrefix = route.sharedElementPrefix,
            startIndex = route.startIndex,
            threadGate = null,
            viewerState = null,
            media = route.model<Embed.Media>().toGalleryMedia(),
            post = Post(
                cid = Constants.unknownPostId,
                uri = PostUri(
                    route.profileId,
                    route.postRecordKey,
                ),
                author = stubProfile(
                    did = route.profileId,
                    handle = ProfileHandle(route.profileId.id),
                ),
                replyCount = 0,
                repostCount = 0,
                likeCount = 0,
                quoteCount = 0,
                indexedAt = Instant.DISTANT_PAST,
                embed = null,
                record = null,
                viewerStats = null,
                labels = emptyList(),
                embeddedRecord = null,
            ),
        ),
    ),
)

val GalleryItem.posterSharedElementPrefix
    get() = "poster-$sharedElementPrefix"

sealed class GalleryItem {

    abstract val post: Post
    abstract val viewerState: ProfileViewerState?
    abstract val startIndex: Int
    abstract val media: List<Media>
    abstract val threadGate: ThreadGate?
    abstract val sharedElementPrefix: String

    data class Initial(
        override val post: Post,
        override val viewerState: ProfileViewerState?,
        override val startIndex: Int,
        override val media: List<Media>,
        override val threadGate: ThreadGate?,
        override val sharedElementPrefix: String,
    ) : GalleryItem()

    data class Tiled(
        override val post: Post,
        override val viewerState: ProfileViewerState?,
        override val startIndex: Int,
        override val media: List<Media>,
        override val threadGate: ThreadGate?,
        override val sharedElementPrefix: String,
    ) : GalleryItem()

    sealed class Media {
        data class Photo(
            val image: EmbeddedImage,
        ) : Media()

        data class Video(
            val video: EmbeddedVideo,
        ) : Media()
    }
}

val GalleryItem.Media.key
    get() = when (this) {
        is GalleryItem.Media.Photo -> image.thumb.uri
        is GalleryItem.Media.Video -> video.playlist.uri
    }

internal fun Embed?.toGalleryMedia(): List<GalleryItem.Media> =
    when (this) {
        is ImageList -> this.images.map(GalleryItem.Media::Photo)
        is Video -> listOf(GalleryItem.Media.Video(this))
        is ExternalEmbed,
        UnknownEmbed,
        null,
        -> emptyList()
    }

sealed class Action(val key: String) {

    data class UpdateMutedWord(
        val mutedWordPreference: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord")

    data class BlockAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "BlockAccount")

    data class MuteAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "MuteAccount")

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
