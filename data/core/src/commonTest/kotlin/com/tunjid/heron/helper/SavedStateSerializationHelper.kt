package com.tunjid.heron.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSource

@OptIn(ExperimentalSerializationApi::class)
object SavedStateSerializationHelper {
    val proto = ProtoBuf

    inline fun <reified T> encode(value: T, serializer: KSerializer<T>): ByteArray =
        proto.encodeToByteArray(serializer, value)
}

fun ByteArray.toBufferedSource(): BufferedSource =
    Buffer().apply { write(this@toBufferedSource) }
