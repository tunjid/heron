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
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

typealias ContentLabelPreferences = List<ContentLabelPreference>

@Serializable
data class Preferences(
    @ProtoNumber(1) val local: Local = Local(),
    @ProtoNumber(2) val allowAdultContent: Boolean = false,
    @ProtoNumber(3) val timelinePreferences: List<TimelinePreference> = emptyList(),
    @ProtoNumber(4) val contentLabelPreferences: ContentLabelPreferences = emptyList(),
    @ProtoNumber(5) val labelerPreferences: List<LabelerPreference> = emptyList(),
    @ProtoNumber(6) val hiddenPostPreferences: List<HiddenPostPreference> = emptyList(),
    @ProtoNumber(7) val mutedWordPreferences: List<MutedWordPreference> = emptyList(),
    @ProtoNumber(8) val declaredAgePreferences: DeclaredAgePreference? = null,
    @ProtoNumber(9) val threadViewPreferences: ThreadViewPreference? = null,
    @ProtoNumber(10) val postInteractionSettings: PostInteractionSettingsPreference? = null,
    @ProtoNumber(11) val verificationPreferences: VerificationPreference? = null,
) : UrlEncodableModel {

    @Serializable
    data class Local(
        @ProtoNumber(1) val lastViewedHomeTimelineUri: Uri? = null,
        @ProtoNumber(2) val refreshHomeTimelineOnLaunch: Boolean = false,
        @ProtoNumber(3) val useDynamicTheming: Boolean = false,
        @ProtoNumber(4) val useCompactNavigation: Boolean = false,
        @ProtoNumber(5) val autoHideBottomNavigation: Boolean = true,
        @ProtoNumber(6) val autoPlayTimelineVideos: Boolean = true,
    )

    companion object {
        val EmptyPreferences =
            Preferences(timelinePreferences = emptyList(), contentLabelPreferences = emptyList())

        val BlueSkyGuestPreferences =
            Preferences(
                timelinePreferences =
                    listOf(
                        TimelinePreference(
                            id = Constants.blueSkyDiscoverFeed.uri,
                            type = "feed",
                            value = Constants.blueSkyDiscoverFeed.uri,
                            pinned = true,
                        ),
                        TimelinePreference(
                            id = Constants.heronsFeed.uri,
                            type = "feed",
                            value = Constants.heronsFeed.uri,
                            pinned = true,
                        ),
                    ),
                contentLabelPreferences = emptyList(),
            )

        val BlackSkyGuestPreferences =
            Preferences(
                timelinePreferences =
                    listOf(
                        TimelinePreference(
                            id = Constants.blackSkyTrendingFeed.uri,
                            type = "feed",
                            value = Constants.blackSkyTrendingFeed.uri,
                            pinned = true,
                        ),
                        TimelinePreference(
                            id = Constants.heronsFeed.uri,
                            type = "feed",
                            value = Constants.heronsFeed.uri,
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

val TimelinePreference.timelineRecordUri: RecordUri?
    get() =
        when (type) {
            "feed" -> FeedGeneratorUri(value)
            "list" -> ListUri(value)
            else -> null
        }

@Serializable
data class ContentLabelPreference(
    val labelerId: ProfileId?,
    val label: Label.Value,
    val visibility: Label.Visibility,
)

@Serializable data class LabelerPreference(val labelerCreatorId: ProfileId)

@Serializable data class HiddenPostPreference(val uri: PostUri)

@Serializable
data class MutedWordPreference(
    val value: String,
    val targets: List<Target>,
    val actorTarget: Target? = null,
    val expiresAt: Instant? = null,
) {
    @JvmInline @Serializable value class Target(val value: String)

    companion object {
        val ContentTarget = Target("content")
        val TagTarget = Target("tag")
    }
}

@Serializable data class DeclaredAgePreference(val minAge: Int? = null)

@Serializable data class ThreadViewPreference(val sort: String? = null)

@Serializable
data class PostInteractionSettingsPreference(
    val threadGateAllowed: ThreadGate.Allowed? = null,
    val allowedEmbeds: PostGate.AllowedEmbeds? = null,
)

@Serializable data class VerificationPreference(val hideBadges: Boolean = false)
