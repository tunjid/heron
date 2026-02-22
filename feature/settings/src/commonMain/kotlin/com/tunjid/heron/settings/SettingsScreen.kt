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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedPreference
import com.tunjid.heron.data.core.models.FeedPreference.Companion.homeFeedOrDefault
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideQuotes
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideReplies
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideReposts
import com.tunjid.heron.scaffold.navigation.moderationDestination
import com.tunjid.heron.scaffold.navigation.notificationSettingsDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.NestedNavigation
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.settings.ui.AccountSwitchingItem
import com.tunjid.heron.settings.ui.AppearanceItem
import com.tunjid.heron.settings.ui.ContentAndMediaItem
import com.tunjid.heron.settings.ui.FeedbackItem
import com.tunjid.heron.settings.ui.ModerationItem
import com.tunjid.heron.settings.ui.NotificationSettingsItem
import com.tunjid.heron.settings.ui.OpenSourceLibrariesItem
import com.tunjid.heron.settings.ui.SettingsToggleItem
import com.tunjid.heron.settings.ui.SignOutItem
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.timeline_preferences_quote_reposts
import heron.feature.settings.generated.resources.timeline_preferences_replies
import heron.feature.settings.generated.resources.timeline_preferences_reposts
import org.jetbrains.compose.resources.stringResource

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
                    .animateBounds(paneScaffoldState),
                signedInProfilePreferences = signedInProfilePreferences,
                setRefreshHomeTimelineOnLaunch = {
                    actions(Action.SetRefreshHomeTimelinesOnLaunch(it))
                },
                setAutoplayTimelineVideos = {
                    actions(Action.SetAutoPlayTimelineVideos(it))
                },
                onFeedPreferenceSectionSelected = {
                    actions(Action.UpdateSection(Section.FeedPreferences(it)))
                },
                setShowPostEngagementMetrics = {
                    actions(Action.SetShowPostEngagementMetrics(it))
                },
            )
            ModerationItem(
                modifier = Modifier
                    .animateBounds(paneScaffoldState),
            ) {
                actions(Action.Navigate.To(moderationDestination()))
            }
            NotificationSettingsItem(
                modifier = Modifier
                    .animateBounds(paneScaffoldState),
            ) {
                actions(Action.Navigate.To(notificationSettingsDestination()))
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
                setAutoHideBottomNavigation = {
                    actions(Action.SetAutoHideBottomNavigation(it))
                },
            )
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
}

@Composable
private fun FeedPreferencesSection(
    feedPreference: FeedPreference,
    onFeedPreferenceUpdated: (FeedPreference) -> Unit,
) {
    SettingsToggleItem(
        modifier = Modifier
            .fillMaxWidth(),
        text = stringResource(Res.string.timeline_preferences_replies),
        enabled = true,
        checked = feedPreference.shouldHideReplies,
        onCheckedChange = {
            onFeedPreferenceUpdated(feedPreference.copy(hideReplies = it))
        },
    )
    SettingsToggleItem(
        modifier = Modifier
            .fillMaxWidth(),
        text = stringResource(Res.string.timeline_preferences_reposts),
        enabled = true,
        checked = feedPreference.shouldHideReposts,
        onCheckedChange = {
            onFeedPreferenceUpdated(feedPreference.copy(hideReposts = it))
        },
    )
    SettingsToggleItem(
        modifier = Modifier
            .fillMaxWidth(),
        text = stringResource(Res.string.timeline_preferences_quote_reposts),
        enabled = true,
        checked = feedPreference.shouldHideQuotes,
        onCheckedChange = {
            onFeedPreferenceUpdated(feedPreference.copy(hideQuotePosts = it))
        },
    )
}
