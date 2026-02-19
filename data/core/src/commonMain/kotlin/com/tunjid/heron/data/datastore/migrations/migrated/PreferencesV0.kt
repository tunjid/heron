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

package com.tunjid.heron.data.datastore.migrations.migrated

import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.HiddenPostPreference
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("com.tunjid.heron.data.core.models.Preferences")
internal data class PreferencesV0(
    @ProtoNumber(1) val timelinePreferences: List<TimelinePreference>,
    // Needs default value for serialization to disk
    @ProtoNumber(2) val contentLabelPreferences: ContentLabelPreferences = emptyList(),
    @ProtoNumber(3) val lastViewedHomeTimelineUri: Uri? = null,
    @ProtoNumber(4) val refreshHomeTimelineOnLaunch: Boolean = false,
    @ProtoNumber(5) val labelerPreferences: List<LabelerPreference> = emptyList(),
    @ProtoNumber(6) val allowAdultContent: Boolean = false,
    @ProtoNumber(7) val hiddenPostPreferences: List<HiddenPostPreference> = emptyList(),
    @ProtoNumber(8) val mutedWordPreferences: List<MutedWordPreference> = emptyList(),
    @ProtoNumber(9) val useDynamicTheming: Boolean = false,
    @ProtoNumber(10) val useCompactNavigation: Boolean = false,
) {
    fun asPreferences() =
        Preferences(
            timelinePreferences = timelinePreferences,
            contentLabelPreferences = contentLabelPreferences,
            labelerPreferences = labelerPreferences,
            allowAdultContent = allowAdultContent,
            hiddenPostPreferences = hiddenPostPreferences,
            mutedWordPreferences = mutedWordPreferences,
            local =
                Preferences.Local(
                    lastViewedHomeTimelineUri = lastViewedHomeTimelineUri,
                    refreshHomeTimelineOnLaunch = refreshHomeTimelineOnLaunch,
                    useDynamicTheming = useDynamicTheming,
                    useCompactNavigation = useCompactNavigation,
                ),
        )
}
