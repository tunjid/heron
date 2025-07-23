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

package com.tunjid.heron.conversation

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.repository.MessageQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    @Transient
    val signedInProfile: Profile? = null,
    val id: ConversationId,
    val members: List<Profile> = emptyList(),
    override val tilingData: TilingState.Data<MessageQuery, Message>,
    @Transient
    val messages: List<String> = emptyList(),
) : TilingState<MessageQuery, Message>


sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action(key = "Tile")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(), NavigationAction by delegate
    }
}

fun State(
    conversationId: ConversationId,
    members: List<Profile>,
) = State(
    id = conversationId,
    members = members,
    tilingData = TilingState.Data(
        currentQuery = MessageQuery(
            conversationId = conversationId,
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
                limit = 15
            )
        )
    )
)