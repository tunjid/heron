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
class DidDocSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: SavedState.AuthTokens.DidDoc = burstValues(
        SavedState.AuthTokens.DidDoc(),
        SampleSavedStateData.didDoc(),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = SavedState.AuthTokens.DidDoc.serializer()
        )
        assertEquals(original, decoded)
    }
}
