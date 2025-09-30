package com.tunjid.heron.models.savedState

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.SampleSavedStateData
import com.tunjid.heron.helper.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Burst
class ProfileDataSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: SavedState.ProfileData = burstValues(
        SavedState.ProfileData(
            preferences = Preferences(
                timelinePreferences = emptyList(),
                contentLabelPreferences = emptyList(),
                lastViewedHomeTimelineUri = GenericUri(""),
            ),
            notifications = SavedState.Notifications(),
            writes = SavedState.Writes(),
        ),
        SampleSavedStateData.profileData(),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = SavedState.ProfileData.serializer(),
        )
        assertEquals(original, decoded)
    }
}
