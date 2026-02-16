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

package com.tunjid.heron.settings

import com.mikepenz.aboutlibraries.Libs
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val isSwitchingAccount: Boolean = false,
    val switchingToProfile: ProfileId? = null,
    val signedInProfilePreferences: Preferences? = null,
    val openSourceLibraries: Libs? = null,
    val pastSessions: List<SessionSummary> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

sealed class Action(val key: String) {

    data class SwitchSession(
        val sessionSummary: SessionSummary,
    ) : Action(key = "SwitchSession")

    data class SetRefreshHomeTimelinesOnLaunch(
        val refreshHomeTimelinesOnLaunch: Boolean,
    ) : Action(key = "SetRefreshHomeTimelinesOnLaunch")

    data class SetAutoPlayTimelineVideos(
        val autoPlayTimelineVideos: Boolean,
    ) : Action(key = "SetAutoPlayTimelineVideos")

    data class SetDynamicThemingPreference(
        val dynamicTheming: Boolean,
    ) : Action(key = "SetDynamicThemingPreference")

    data class SetCompactNavigation(
        val compactNavigation: Boolean,
    ) : Action(key = "SetCompactNavigation")

    data class SetAutoHideBottomNavigation(
        val autoHideBottomNavigation: Boolean,
    ) : Action(key = "SetAutoHideBottomNavigation")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data object SignOut : Action(key = "SignOut")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        /** Handles navigation to settings child screens */
        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
