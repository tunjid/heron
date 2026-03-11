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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.FeedPreference
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideQuotes
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideReplies
import com.tunjid.heron.data.core.models.FeedPreference.Companion.shouldHideReposts
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.timeline_preferences_quote_reposts
import heron.feature.settings.generated.resources.timeline_preferences_replies
import heron.feature.settings.generated.resources.timeline_preferences_reposts
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedPreferencesSection(
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
