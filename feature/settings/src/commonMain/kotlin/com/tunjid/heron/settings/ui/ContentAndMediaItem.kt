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

package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedPreference.Companion.homeFeedOrDefault
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.settings.Section
import com.tunjid.heron.ui.icons.Article
import com.tunjid.heron.ui.icons.Forum
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.Home
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.auto_play_timeline_videos
import heron.feature.settings.generated.resources.content_and_media
import heron.feature.settings.generated.resources.following_feed_preferences
import heron.feature.settings.generated.resources.refresh_timelines_on_launch
import heron.feature.settings.generated.resources.show_post_engagement_metrics
import heron.feature.settings.generated.resources.show_trending_topics
import heron.feature.settings.generated.resources.thread_preferences
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContentAndMediaItem(
    modifier: Modifier = Modifier,
    signedInProfilePreferences: Preferences,
    setRefreshHomeTimelineOnLaunch: (Boolean) -> Unit,
    setAutoplayTimelineVideos: (Boolean) -> Unit,
    setShowPostEngagementMetrics: (Boolean) -> Unit,
    setShowTrendingTopics: (Boolean) -> Unit,
    onSectionSelected: (Section) -> Unit,
) {
    ExpandableSettingsItemRow(
        modifier = modifier
            .settingsItemPaddingAndMinHeight()
            .fillMaxWidth(),
        title = stringResource(Res.string.content_and_media),
        icon = HeronIcons.Article,
    ) {
        SettingsItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSectionSelected(
                        Section.FeedPreferences(
                            signedInProfilePreferences.feedPreferences.homeFeedOrDefault(),
                        ),
                    )
                }
                .settingsItemPaddingAndMinHeight(),
            icon = HeronIcons.Home,
            title = stringResource(Res.string.following_feed_preferences),
        )
        SettingsItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSectionSelected(
                        Section.ThreadPreferences(
                            signedInProfilePreferences.threadViewPreferences,
                        ),
                    )
                }
                .settingsItemPaddingAndMinHeight(),
            icon = HeronIcons.Forum,
            title = stringResource(Res.string.thread_preferences),
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 8.dp),
        )

        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.refresh_timelines_on_launch),
            enabled = true,
            checked = signedInProfilePreferences.local.refreshHomeTimelineOnLaunch,
            onCheckedChange = setRefreshHomeTimelineOnLaunch,
        )
        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.auto_play_timeline_videos),
            enabled = true,
            checked = signedInProfilePreferences.local.autoPlayTimelineVideos,
            onCheckedChange = setAutoplayTimelineVideos,
        )
        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.show_post_engagement_metrics),
            enabled = true,
            checked = signedInProfilePreferences.local.showPostEngagementMetrics,
            onCheckedChange = setShowPostEngagementMetrics,
        )
        SettingsToggleItem(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(Res.string.show_trending_topics),
            enabled = true,
            checked = signedInProfilePreferences.local.showTrendingTopics,
            onCheckedChange = setShowTrendingTopics,
        )
    }
}
