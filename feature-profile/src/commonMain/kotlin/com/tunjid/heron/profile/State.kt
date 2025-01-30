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

package com.tunjid.heron.profile

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.domain.timeline.TimelineStateHolders
import com.tunjid.heron.scaffold.navigation.NavigationAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val profile: Profile,
    val signedInProfileId: Id? = null,
    val isSignedInProfile: Boolean = false,
    val viewerState: ProfileViewerState? = null,
    val avatarSharedElementKey: String,
    @Transient
    val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
    @Transient
    val timelineStateHolders: TimelineStateHolders = TimelineStateHolders(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class ToggleViewerState(
        val signedInProfileId: Id,
        val viewedProfileId: Id,
        val following: Uri?,
        val followedBy: Uri?,
    ) : Action(key = "ToggleViewerState")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}