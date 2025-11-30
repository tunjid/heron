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

package com.tunjid.heron.postdetail

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.appliedLabels
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val anchorPost: Post?,
    val sharedElementPrefix: String,
    @Transient
    val signedInProfileId: ProfileId? = null,
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val items: List<TimelineItem> = listOfNotNull(
        anchorPost?.let {
            TimelineItem.Thread(
                id = it.uri.uri,
                anchorPostIndex = 0,
                posts = listOf(it),
                generation = 0,
                hasBreak = false,
                threadGate = null,
                appliedLabels = it.appliedLabels(
                    adultContentEnabled = false,
                    labelers = emptyList(),
                    labelPreferences = emptyList(),
                ),
            )
        },
    ),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(route: Route) = State(
    anchorPost = route.model as? Post,
    sharedElementPrefix = route.sharedElementPrefix,
)

sealed class Action(val key: String) {

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class UpdateThreadGate(
        val summary: ThreadGate.Summary,
    ) : Action(key = "UpdateThreadGate")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
