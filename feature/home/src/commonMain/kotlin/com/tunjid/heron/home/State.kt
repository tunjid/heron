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
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.ScaffoldMessage
import com.tunjid.heron.timeline.state.TimelineStateHolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val currentTabUri: Uri? = null,
    val tabLayout: TabLayout = TabLayout.Collapsed.All,
    @Transient
    val timelinePreferenceSaveRequestId: String? = null,
    @Transient
    val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
    @Transient
    val timelines: List<Timeline.Home> = emptyList(),
    @Transient
    val timelineStateHolders: List<HomeScreenStateHolders> = emptyList(),
    @Transient
    val signedInProfile: Profile? = null,
    @Transient
    val messages: List<ScaffoldMessage> = emptyList(),
)

@Serializable
sealed class TabLayout {
    data object Expanded : TabLayout()
    sealed class Collapsed : TabLayout() {
        data object All : Collapsed()
        data object Selected : Collapsed()
    }
}

sealed class HomeScreenStateHolders : TimelineStateHolder {

    abstract val mutator: TimelineStateHolder

    data class Pinned(
        override val mutator: TimelineStateHolder,
    ) : HomeScreenStateHolders(),
        TimelineStateHolder by mutator

    data class Saved(
        override val mutator: TimelineStateHolder,
    ) : HomeScreenStateHolders(),
        TimelineStateHolder by mutator
}

sealed class Action(val key: String) {

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: ScaffoldMessage,
    ) : Action(key = "SnackbarDismissed")

    data class SetCurrentTab(
        val currentTabUri: Uri,
    ) : Action(key = "SetCurrentTab")

    data class SetTabLayout(
        val layout: TabLayout,
    ) : Action(key = "SetTabLayout")

    data object RefreshCurrentTab : Action(key = "RefreshCurrentTab")

    sealed class UpdateTimeline : Action(key = "Timeline") {
        data object RequestUpdate : UpdateTimeline()

        data class Update(
            val timelines: List<Timeline.Home>,
        ) : UpdateTimeline()
    }

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
