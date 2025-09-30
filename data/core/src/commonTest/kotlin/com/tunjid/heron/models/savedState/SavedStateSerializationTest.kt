package com.tunjid.heron.models.savedState

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.SampleSavedStateData
import com.tunjid.heron.helper.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Burst
class SavedStateSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: SavedState = burstValues(
        SampleSavedStateData.savedStateWithGuest(),
        SampleSavedStateData.savedStateWithBearer(),
        SampleSavedStateData.savedStateWithDPoP(),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = SavedState.serializer(),
        )
        assertEquals(original, decoded)
    }
}
