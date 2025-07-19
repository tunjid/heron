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

package com.tunjid.heron.home

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.timeline.state.TimelineStateHolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val currentSourceId: String? = null,
    val timelinePreferencesExpanded: Boolean = false,
    @Transient
    val timelinePreferenceSaveRequestId: String? = null,
    @Transient
    val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
    @Transient
    val timelines: List<Timeline.Home> = emptyList(),
    @Transient
    val timelineStateHolders: List<TimelineStateHolder> = emptyList(),
    @Transient
    val signedInProfile: Profile? = null,
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SetCurrentTab(
        val sourceId: String,
    ) : Action(key = "SetCurrentTab")

    data class SetPreferencesExpanded(
        val isExpanded: Boolean,
    ) : Action(key = "SetPreferencesExpanded")

    data object RefreshCurrentTab : Action(key = "RefreshCurrentTab")

    sealed class UpdateTimeline : Action(key = "Timeline") {
        data object RequestUpdate : UpdateTimeline()

        data class Update(
            val timelines: List<Timeline.Home>
        ) : UpdateTimeline()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {

        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}