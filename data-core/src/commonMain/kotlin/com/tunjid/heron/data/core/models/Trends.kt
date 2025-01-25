package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Trend(
    val topic: String,
    val displayName: String?,
    val description: String?,
    val link: String,
)

@Serializable
data class Trends(
    val topics: List<Trend> = emptyList(),
    val suggested: List<Trend> = emptyList(),
)