package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.database.entities.LabelDefinitionEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.protobuf.ProtoBuf

object LabelDefinitionProtoSerializer {
    private val proto = ProtoBuf

    private val listSerializer = ListSerializer(LabelDefinitionEntity.LocaleInfo.serializer())

    fun serialize(locales: List<LabelDefinitionEntity.LocaleInfo>): ByteArray {
        return proto.encodeToByteArray(listSerializer, locales)
    }

    fun deserialize(bytes: ByteArray): List<LabelDefinitionEntity.LocaleInfo> {
        if (bytes.isEmpty()) return emptyList()
        return proto.decodeFromByteArray(listSerializer, bytes)
    }
}
