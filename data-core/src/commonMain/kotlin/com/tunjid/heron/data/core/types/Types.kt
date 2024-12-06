package com.tunjid.heron.data.core.types

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Id(
    val id: String,
) {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class Uri(
    val uri: String,
) {
    override fun toString(): String = uri
}