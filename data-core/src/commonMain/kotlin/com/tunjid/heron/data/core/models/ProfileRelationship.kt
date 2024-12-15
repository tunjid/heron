package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRelationship(
    val blocking: Boolean,
    val muted: Boolean,
    val follows: Boolean,
)