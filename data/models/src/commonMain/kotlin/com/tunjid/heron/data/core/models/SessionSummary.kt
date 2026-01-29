package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SessionSummary(
    val lastSeen: Instant,
    val profileId: ProfileId,
    val profileHandle: ProfileHandle,
    val profileAvatar: ImageUri?,
)
