package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import kotlin.time.Instant

fun sampleProfileData() =
    SavedState.ProfileData(
        preferences = samplePreferences(),
        notifications = sampleNotifications(),
        writes = sampleWrites(),
    )

fun samplePreferences() =
    Preferences(
        timelinePreferences =
            listOf(
                TimelinePreference(
                    id = "feed:discover",
                    type = "feed",
                    value = "https://example.com/feed/discover",
                    pinned = true,
                ),
                TimelinePreference(
                    id = "feed:herons",
                    type = "feed",
                    value = "https://example.com/feed/herons",
                    pinned = false,
                ),
            ),
        contentLabelPreferences =
            listOf(
                ContentLabelPreference(
                    labelerId = ProfileId("labeler-123"),
                    label = Label.Value("spoiler"),
                    visibility = Label.Visibility.Hide,
                )
            ),
        local =
            Preferences.Local(
                lastViewedHomeTimelineUri = GenericUri("https://example.com/home"),
                refreshHomeTimelineOnLaunch = true,
            ),
    )

fun sampleNotifications() =
    SavedState.Notifications(
        lastRead = Instant.parse("2024-01-01T12:00:00Z"),
        lastRefreshed = Instant.parse("2024-01-01T12:30:00Z"),
    )

fun sampleWrites() = SavedState.Writes(pendingWrites = emptyList(), failedWrites = emptyList())
