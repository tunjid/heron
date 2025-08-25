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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.serialization.Serializable

typealias ContentLabelPreferences = List<ContentLabelPreference>

@Serializable
data class Preferences(
    val timelinePreferences: List<TimelinePreference>,
    // Needs default value for serialization to disk
    val contentLabelPreferences: ContentLabelPreferences = emptyList(),
) : UrlEncodableModel {
    companion object {
        val DefaultPreferences = Preferences(
            timelinePreferences = listOf(
                TimelinePreference(
                    id = Constants.discoverFeed.uri,
                    type = "feed",
                    value = Constants.discoverFeed.uri,
                    pinned = true,
                ),
            ),
            contentLabelPreferences = emptyList(),
        )
    }
}

@Serializable
data class TimelinePreference(
    val id: String,
    val type: String,
    val value: String,
    val pinned: Boolean,
)

val TimelinePreference.feedGeneratorUri: FeedGeneratorUri?
    get() = when (type) {
        "feed" -> FeedGeneratorUri(value)
        else -> null
    }

@Serializable
data class ContentLabelPreference(
    val labelerId: ProfileId?,
    val label: Label.Value,
    val visibility: Label.Visibility,
)
