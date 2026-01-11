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

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.navigation.moderationDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.settings.ui.AppearanceItem
import com.tunjid.heron.settings.ui.ContentAndMediaItem
import com.tunjid.heron.settings.ui.FeedbackItem
import com.tunjid.heron.settings.ui.ModerationItem
import com.tunjid.heron.settings.ui.OpenSourceLibrariesItem
import com.tunjid.heron.settings.ui.SignOutItem

@Composable
internal fun SettingsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.signedInProfilePreferences?.let { signedInProfilePreferences ->
            ContentAndMediaItem(
                modifier = Modifier
                    .animateBounds(paneScaffoldState),
                signedInProfilePreferences = signedInProfilePreferences,
                setRefreshHomeTimelineOnLaunch = {
                    actions(Action.SetRefreshHomeTimelinesOnLaunch(it))
                },
            )
            ModerationItem(
                modifier = Modifier
                    .animateBounds(paneScaffoldState),
            ) {
                actions(Action.Navigate.To(moderationDestination()))
            }
            AppearanceItem(
                modifier = Modifier
                    .animateBounds(paneScaffoldState),
                signedInProfilePreferences = signedInProfilePreferences,
                setDynamicThemingPreference = {
                    actions(Action.SetDynamicThemingPreference(it))
                },
                setCompactNavigation = {
                    actions(Action.SetCompactNavigation(it))
                },
                setPersistentBottomNavigation = {
                    actions(Action.SetAutoHideBottomNavigation(it))
                },
            )
        }
        FeedbackItem(
            modifier = Modifier
                .animateBounds(paneScaffoldState),
        )
        OpenSourceLibrariesItem(
            modifier = Modifier
                .animateBounds(paneScaffoldState),
            libraries = state.openSourceLibraries,
        )
        SignOutItem(
            modifier = Modifier
                .animateBounds(paneScaffoldState),
        ) {
            actions(Action.SignOut)
        }
    }
}
