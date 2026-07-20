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

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.model
import com.tunjid.heron.ui.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.TextFieldValueSerializer
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val sharedElementPrefix: String?,
        val postType: Post.Create? = null,
        // The draft currently being edited, if this post was resumed from one. Non-null means a
        // successful post deletes that draft, and a save updates it in place.
        val draftId: DraftId? = null,
        val signedInProfile: Profile? = null,
        val fabExpanded: Boolean = true,
        val embeddedRecord: Record.Embeddable.Native? = null,
        @Transient
        val linkPreview: LinkPreview? = null,
        @Transient
        val isLoadingLinkPreview: Boolean = false,
        @Transient
        val dismissedUri: Uri? = null,
        @Transient
        val interactionsPreference: PostInteractionSettingsPreference? = null,
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
    ) : State

    companion object {
        operator fun invoke(route: Route): Immutable =
            when (val model = route.model<Post.Create>()) {
                is Post.Create -> Immutable(
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

                else -> Immutable(
                    sharedElementPrefix = route.sharedElementPrefix,
                )
            }
    }
}

val State.canDraft
    get() = postType !is Post.Create.Reply && postType !is Post.Create.Quote

val State.hasComposedContent
    get() = postText.text.isNotBlank() || photos.isNotEmpty() || video != null

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

    data class UriDetected(
        val url: String,
    ) : Action("UriDetected")

    data class CreatePost(
        val postType: Post.Create?,
        val authorId: ProfileId,
        val text: String,
        val links: List<Link>,
        val media: List<RestrictedFile.Media>,
        val embeddedRecordReference: Record.Reference?,
        val linkPreview: LinkPreview?,
        val interactionPreference: PostInteractionSettingsPreference?,
        val sourceDraftId: DraftId? = null,
    ) : Action("CreatePost")

    data class LoadDraft(
        val draft: Post.Draft,
    ) : Action("LoadDraft")

    data object SaveDraft : Action("SaveDraft")

    data class SetFabExpanded(
        val expanded: Boolean,
    ) : Action("SetFabExpanded")

    data class UpdateInteractionSettings(
        val interactionSettingsPreference: PostInteractionSettingsPreference?,
    ) : Action("UpdateInteractionSettings")

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

    data class RemoveDetectedUri(
        val uri: Uri,
    ) : Action("RemoveDetectedUri")
}
