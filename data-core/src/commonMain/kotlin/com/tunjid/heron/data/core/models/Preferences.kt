package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Preferences(
    val timelinePreferences: List<TimelinePreference>,
) : ByteSerializable

@Serializable
data class TimelinePreference(
    val id: String,
    val type: String,
    val value: String,
    val pinned: Boolean,
)