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

package com.tunjid.heron.list

import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.treenav.pop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val creator: Profile? = null,
    @Transient
    val timelineState: TimelineState? = null,
    @Transient
    val timelineStateHolder: TimelineStateHolder? = null,
    @Transient
    val membersStateHolder: MembersStateHolder? = null,
    @Transient
    val messages: List<String> = emptyList(),
)

data class MemberState(
    val signedInProfileId: ProfileId?,
    override val tilingData: TilingState.Data<ListMemberQuery, ListMember>,
) : TilingState<ListMemberQuery, ListMember>

typealias MembersStateHolder = ActionStateMutator<ListMemberQuery, StateFlow<MemberState>>

sealed class Action(val key: String) {

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: GenericUri?,
        val followedBy: GenericUri?,
    ) : Action(key = "ToggleViewerState")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}