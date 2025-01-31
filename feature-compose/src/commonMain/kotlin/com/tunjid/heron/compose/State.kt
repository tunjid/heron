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

import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val sharedElementPrefix: String?,
    val postType: Post.Create? = null,
    val signedInProfile: Profile? = null,
    val fabExpanded: Boolean = true,
    @Transient // TODO: Write a custom serializer for this
    val postText: TextFieldValue = TextFieldValue(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    data class PostTextChanged(
        val textFieldValue: TextFieldValue,
    ) : Action("PostTextChanged")

    data class CreatePost(
        val postType: Post.Create?,
        val authorId: Id,
        val text: String,
        val links: List<Post.Link>,
    ) : Action("CreatePost")

    data class SetFabExpanded(
        val expanded: Boolean,
    ) : Action("SetFabExpanded")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}