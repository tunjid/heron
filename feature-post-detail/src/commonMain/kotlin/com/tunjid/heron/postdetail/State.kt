/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.postdetail

import com.tunjid.heron.data.core.models.PostThread
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    @Transient
    val items: List<TimelineItem> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Common.Pop

        data class ToProfile(
            val profileId: Id,
            val profileAvatar: Uri?,
            val avatarSharedElementKey: String?,
        ) : Navigate(), NavigationAction by NavigationAction.Common.ToProfile(
            profileId = profileId,
            profileAvatar = profileAvatar,
            avatarSharedElementKey = avatarSharedElementKey,
        )

        data class ToPost(
            val postUri: Uri,
            val postId: Id,
            val profileId: Id,
        ) : Navigate(), NavigationAction by NavigationAction.Common.ToPost(
            postUri = postUri,
            postId = postId,
            profileId = profileId
        )
    }
}