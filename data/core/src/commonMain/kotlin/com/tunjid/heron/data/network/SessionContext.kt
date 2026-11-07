package com.tunjid.heron.data.network

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext

internal sealed class SessionContext : CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = SessionContext

    abstract val profileId: ProfileId?
    abstract val profileData: SavedState.ProfileData

    data class Current(
        // Null for guests or when signed out
        override val profileId: ProfileId?,
        override val profileData: SavedState.ProfileData,
    ) : SessionContext()

    data class Previous(
        override val profileId: ProfileId,
        override val profileData: SavedState.ProfileData,
    ) : SessionContext()

    companion object Key : CoroutineContext.Key<SessionContext>
}

internal suspend fun currentSessionContext() =
    currentCoroutineContext()[SessionContext.Key]
