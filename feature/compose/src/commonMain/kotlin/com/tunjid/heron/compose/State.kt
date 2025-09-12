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

package com.tunjid.heron.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.scaffold.scaffold.SnackbarMessage
import com.tunjid.treenav.strings.Route
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val sharedElementPrefix: String?,
    val postType: Post.Create? = null,
    val signedInProfile: Profile? = null,
    val fabExpanded: Boolean = true,
    val quotedPost: Post? = null,
    val labelPreferences: ContentLabelPreferences,
    val labelers: List<Labeler>,
    @Transient // TODO: Write a custom serializer for this
    val postText: TextFieldValue = TextFieldValue(),
    @Transient
    val photos: List<MediaItem.Photo> = emptyList(),
    @Transient
    val video: MediaItem.Video? = null,
    @Transient
    val messages: List<SnackbarMessage> = emptyList(),
)

fun State(route: Route): State = when (val model = route.model) {
    is Post.Create -> State(
        postText = TextFieldValue(
            annotatedString = AnnotatedString(
                when (model) {
                    is Post.Create.Mention -> "@${model.profile.handle.id} "
                    is Post.Create.Reply,
                    is Post.Create.Quote,
                    Post.Create.Timeline,
                    -> ""
                },
            ),
            selection = TextRange(
                if (model is Post.Create.Mention) model.profile.handle.id.length + 2
                else 0,
            ),
        ),
        sharedElementPrefix = route.sharedElementPrefix,
        labelers = emptyList(),
        labelPreferences = emptyList(),
        postType = model,
    )

    else -> State(
        sharedElementPrefix = route.sharedElementPrefix,
        labelers = emptyList(),
        labelPreferences = emptyList(),
    )
}

val State.hasLongPost
    get() = when (val type = postType) {
        is Post.Create.Mention -> type.profile.handle.id.length
        is Post.Create.Reply -> type.parent.record?.text?.length ?: 0
        is Post.Create.Quote -> 180
        Post.Create.Timeline -> 0
        null -> 0
    } + postText.text.length > 180

// On Android, a content resolver may be invoked for the path.
// Make sure it is only ever invoked off the main thread by enforcing with this class
sealed class MediaItem(
    val path: String?,
) {

    abstract val file: PlatformFile
    abstract val size: IntSize

    class Photo(
        override val file: PlatformFile,
        override val size: IntSize = IntSize.Zero,
    ) : MediaItem(file.path)

    class Video(
        override val file: PlatformFile,
        override val size: IntSize = IntSize.Zero,
    ) : MediaItem(file.path)

    fun updateSize(
        size: IntSize,
    ) = when (this) {
        is Photo -> Photo(
            file = file,
            size = size,
        )

        is Video -> Video(
            file = file,
            size = size,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MediaItem

        if (path != other.path) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path?.hashCode() ?: 0
        result = 31 * result + size.hashCode()
        return result
    }
}

sealed class Action(val key: String) {

    data class PostTextChanged(
        val textFieldValue: TextFieldValue,
    ) : Action("PostTextChanged")

    data class CreatePost(
        val postType: Post.Create?,
        val authorId: ProfileId,
        val text: String,
        val links: List<Link>,
        val media: List<MediaItem>,
    ) : Action("CreatePost")

    data class SetFabExpanded(
        val expanded: Boolean,
    ) : Action("SetFabExpanded")

    sealed class EditMedia : Action("EditMedia") {
        data class AddPhotos(
            val photos: List<PlatformFile>,
        ) : EditMedia()

        data class AddVideo(
            val video: PlatformFile?,
        ) : EditMedia()

        data class RemoveMedia(
            val media: MediaItem?,
        ) : EditMedia()

        data class UpdateMedia(
            val media: MediaItem?,
        ) : EditMedia()
    }

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}
