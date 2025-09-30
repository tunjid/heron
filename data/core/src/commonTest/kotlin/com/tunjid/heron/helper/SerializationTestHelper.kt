package com.tunjid.heron.helper

import com.tunjid.heron.data.datastore.migrations.VersionedSavedState
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.BearerSavedState
import com.tunjid.heron.fakes.DPoPSavedState
import com.tunjid.heron.fakes.GuestSavedState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object SerializationTestHelper {

    private val savedStateModule = SerializersModule {
        polymorphic(SavedState::class) {
            subclass(GuestSavedState::class, GuestSavedState.serializer())
            subclass(BearerSavedState::class, BearerSavedState.serializer())
            subclass(DPoPSavedState::class, DPoPSavedState.serializer())
            subclass(VersionedSavedState::class, VersionedSavedState.serializer())
        }
    }

    private val cbor = Cbor {
        ignoreUnknownKeys = true
        serializersModule = savedStateModule
    }

    private val proto = ProtoBuf {
        serializersModule = savedStateModule
    }

    enum class Format { CBOR, PROTOBUF }

    fun <T> encode(format: Format, value: T, serializer: KSerializer<T>): ByteArray =
        when (format) {
            Format.CBOR -> cbor.encodeToByteArray(serializer, value)
            Format.PROTOBUF -> proto.encodeToByteArray(serializer, value)
        }

    fun <T> decode(format: Format, bytes: ByteArray, serializer: KSerializer<T>): T =
        when (format) {
            Format.CBOR -> cbor.decodeFromByteArray(serializer, bytes)
            Format.PROTOBUF -> proto.decodeFromByteArray(serializer, bytes)
        }

    fun <T> roundTrip(format: Format, value: T, serializer: KSerializer<T>): T =
        decode(format, encode(format, value, serializer), serializer)
}
