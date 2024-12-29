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

package com.tunjid.heron.profile

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.domain.timeline.TimelineStateHolder
import com.tunjid.heron.scaffold.navigation.NavigationAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val profile: Profile,
    val isSignedInProfile: Boolean = false,
    val profileRelationship: ProfileRelationship? = null,
    val avatarSharedElementKey: String,
    @Transient
    val pageWithUpdates: Int = -1,
    @Transient
    val timelines: List<Timeline.Profile> = emptyList(),
    @Transient
    val timelineStateHolders: List<TimelineStateHolder> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    data class UpdatePageWithUpdates(
        val sourceIdWithUpdates: String?,
    ) : Action(key = "UpdatePageWithUpdates")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}