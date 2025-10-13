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

package com.tunjid.heron.editprofile

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.editprofile.di.profileHandleOrId
import com.tunjid.heron.media.picker.MediaItem
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.avatarSharedElementKey
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.ui.text.FormField
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.Validator
import com.tunjid.heron.ui.text.valueFor
import com.tunjid.treenav.strings.Route
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.character_limit
import heron.feature.edit_profile.generated.resources.description
import heron.feature.edit_profile.generated.resources.display_name
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal val DisplayName = FormField.Id("displayName")
internal val Description = FormField.Id("description")

@Serializable
data class State(
    val profile: Profile,
    val avatarSharedElementKey: String,
    @Transient val updatedAvatar: MediaItem.Photo? = null,
    @Transient val updatedBanner: MediaItem.Photo? = null,
    @Transient
    val fields: List<FormField> = listOf(
        FormField(
            id = DisplayName,
            value = "",
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            contentDescription = Memo.Resource(Res.string.display_name),
            validator = Validator(
                String::isMaxDisplayName to Memo.Resource(
                    Res.string.character_limit,
                    listOf(Res.string.display_name),
                ),
            ),
        ),
        FormField(
            id = Description,
            value = "",
            maxLines = Int.MAX_VALUE,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            contentDescription = Memo.Resource(Res.string.description),
            validator = Validator(
                String::isMaxDescription to Memo.Resource(
                    Res.string.character_limit,
                    listOf(Res.string.description),
                ),
            ),
        ),
    ),
    @Transient
    val messages: List<Memo> = emptyList(),
)

@OptIn(ExperimentalUuidApi::class)
fun State(route: Route) = State(
    profile = (route.model as? Profile) ?: stubProfile(
        did = ProfileId(route.profileHandleOrId.id),
        handle = ProfileHandle(route.profileHandleOrId.id),
        avatar = null,
    ),
    avatarSharedElementKey = route.avatarSharedElementKey ?: Uuid.random().toString(),
)

internal fun State.saveProfileAction() = Action.SaveProfile(
    profileId = profile.did,
    displayName = fields.valueFor(DisplayName),
    description = fields.valueFor(Description),
    avatar = updatedAvatar,
    banner = updatedBanner,
)

sealed class Action(val key: String) {
    data class AvatarPicked(
        val item: MediaItem.Photo,
    ) : Action(key = "AvatarPicked")

    data class BannerPicked(
        val item: MediaItem.Photo,
    ) : Action(key = "BannerPicked")

    data class SaveProfile(
        val profileId: ProfileId,
        val displayName: String,
        val description: String,
        val avatar: MediaItem.Photo?,
        val banner: MediaItem.Photo?,
    ) : Action(key = "SaveProfile")

    data class FieldChanged(
        val id: FormField.Id,
        val text: String,
    ) : Action("FieldChanged")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}

private val String.isMaxDisplayName get() = length <= MAX_DISPLAY_NAME_LENGTH
private val String.isMaxDescription get() = length <= MAX_DESCRIPTION_LENGTH

private const val MAX_DISPLAY_NAME_LENGTH = 64
private const val MAX_DESCRIPTION_LENGTH = 256
