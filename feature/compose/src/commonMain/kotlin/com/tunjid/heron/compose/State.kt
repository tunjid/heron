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
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.TextFieldValueSerializer
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val sharedElementPrefix: String?,
    val postType: Post.Create? = null,
    val signedInProfile: Profile? = null,
    val fabExpanded: Boolean = true,
    val embeddedRecord: Record? = null,
    @Serializable(with = TextFieldValueSerializer::class)
    val postText: TextFieldValue = TextFieldValue(),
    @Transient
    val photos: List<RestrictedFile.Media.Photo> = emptyList(),
    @Transient
    val video: RestrictedFile.Media.Video? = null,
    @Transient
    val messages: List<Memo> = emptyList(),
    @Transient
    val suggestedProfiles: List<Profile> = emptyList(),
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
        postType = model,
    )

    else -> State(
        sharedElementPrefix = route.sharedElementPrefix,
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

sealed class Action(val key: String) {

    data class PostTextChanged(
        val textFieldValue: TextFieldValue,
    ) : Action("PostTextChanged")

    data class CreatePost(
        val postType: Post.Create?,
        val authorId: ProfileId,
        val text: String,
        val links: List<Link>,
        val media: List<RestrictedFile.Media>,
        val embeddedRecordReference: Record.Reference?,
    ) : Action("CreatePost")

    data class SetFabExpanded(
        val expanded: Boolean,
    ) : Action("SetFabExpanded")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class EditMedia : Action("EditMedia") {
        data class AddPhotos(
            val photos: List<RestrictedFile.Media.Photo>,
        ) : EditMedia()

        data class AddVideo(
            val video: RestrictedFile.Media.Video?,
        ) : EditMedia()

        data class RemoveMedia(
            val media: RestrictedFile.Media?,
        ) : EditMedia()

        data class UpdateMedia(
            val media: RestrictedFile.Media?,
        ) : EditMedia()
    }

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }

    data class SearchProfiles(val query: String) : Action("SearchProfiles")

    data object ClearSuggestions : Action("ClearSuggestions")

    data object RemoveEmbeddedRecord : Action("RemoveEmbeddedRecord")
}
