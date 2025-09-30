package com.tunjid.heron.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object SerializationTestHelper {

    private val cbor = Cbor { ignoreUnknownKeys = true }
    private val proto = ProtoBuf

    enum class Format {
        CBOR,
        PROTOBUF,
    }

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
