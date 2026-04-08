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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedPreference.Companion.homeFeedOrDefault
import com.tunjid.heron.scaffold.navigation.moderationDestination
import com.tunjid.heron.scaffold.navigation.notificationSettingsDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.navigation.standardSubscriptionsDestination
import com.tunjid.heron.scaffold.scaffold.NestedNavigation
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.settings.ui.AccountSwitchingItem
import com.tunjid.heron.settings.ui.AppearanceItem
import com.tunjid.heron.settings.ui.ContentAndMediaItem
import com.tunjid.heron.settings.ui.FeedPreferencesSection
import com.tunjid.heron.settings.ui.FeedbackItem
import com.tunjid.heron.settings.ui.ModerationItem
import com.tunjid.heron.settings.ui.NotificationSettingsItem
import com.tunjid.heron.settings.ui.OpenSourceLibrariesItem
import com.tunjid.heron.settings.ui.PublicationSubscriptionsItem
import com.tunjid.heron.settings.ui.SignOutItem
import com.tunjid.heron.settings.ui.ThreadPreferencesSection

@Composable
internal fun SettingsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    paneScaffoldState.NestedNavigation(
        modifier = modifier
            .fillMaxSize(),
        key = state.section.key,
        content = { key ->
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp,
                    )
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when (key) {
                    Section.Main -> MainSection(
                        state = state,
                        actions = actions,
                        paneScaffoldState = paneScaffoldState,
                    )
                    Section.FeedPreferences -> FeedPreferencesSection(
                        feedPreference = state.signedInProfilePreferences
                            ?.feedPreferences
                            .orEmpty()
                            .homeFeedOrDefault(),
                        onFeedPreferenceUpdated = {
                            actions(Action.UpdateFeedPreference(it))
                        },
                    )
                    Section.ThreadPreferences -> ThreadPreferencesSection(
                        threadViewPreference = state.signedInProfilePreferences
                            ?.threadViewPreferences,
                        onPreferenceUpdated = {
                            actions(Action.UpdateThreadViewPreference(it))
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun MainSection(
    state: State,
    actions: (Action) -> Unit,
    paneScaffoldState: PaneScaffoldState,
) {
    state.signedInProfilePreferences?.let { signedInProfilePreferences ->
        AccountSwitchingItem(
            modifier = Modifier,
            sessionSummaries = state.pastSessions,
            switchPhase = state.switchPhase,
            activeProfileId = state.activeProfileId,
            switchingSession = state.switchingSession,
            onAddAccountClick = {
                actions(Action.Navigate.To(signInDestination()))
            },
            onAccountSelected = { session ->
                actions(Action.SwitchSession(session))
            },
            paneScaffoldState = paneScaffoldState,
        )
        if (state.switchPhase == AccountSwitchPhase.IDLE) {
            ContentAndMediaItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
                signedInProfilePreferences = signedInProfilePreferences,
                setRefreshHomeTimelineOnLaunch = {
                    actions(Action.SetRefreshHomeTimelinesOnLaunch(it))
                },
                setAutoplayTimelineVideos = {
                    actions(Action.SetAutoPlayTimelineVideos(it))
                },
                onSectionSelected = {
                    actions(Action.UpdateSection(it))
                },
                setShowPostEngagementMetrics = {
                    actions(Action.SetShowPostEngagementMetrics(it))
                },
                setShowTrendingTopics = {
                    actions(Action.SetShowTrendingTopics(it))
                },
            )
            ModerationItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
            ) {
                actions(Action.Navigate.To(moderationDestination()))
            }
            NotificationSettingsItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
            ) {
                actions(Action.Navigate.To(notificationSettingsDestination()))
            }
            PublicationSubscriptionsItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
            ) {
                actions(Action.Navigate.To(standardSubscriptionsDestination()))
            }
            AppearanceItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
                signedInProfilePreferences = signedInProfilePreferences,
                setDynamicThemingPreference = {
                    actions(Action.SetDynamicThemingPreference(it))
                },
                setCompactNavigation = {
                    actions(Action.SetCompactNavigation(it))
                },
                setAutoHideBottomNavigation = {
                    actions(Action.SetAutoHideBottomNavigation(it))
                },
            )
            FeedbackItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
            )
            OpenSourceLibrariesItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
                libraries = state.openSourceLibraries,
            )
            SignOutItem(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = paneScaffoldState,
                        boundsTransform = paneScaffoldState.childBoundsTransform,
                    ),
            ) {
                actions(Action.SignOut)
            }
        }
    }
}
