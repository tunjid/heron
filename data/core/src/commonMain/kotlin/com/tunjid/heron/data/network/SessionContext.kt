package com.tunjid.heron.data.network

import com.tunjid.heron.data.repository.SavedState
import kotlin.coroutines.CoroutineContext

internal sealed class SessionContext : CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = SessionContext

    abstract val tokens: SavedState.AuthTokens?
    abstract val profileData: SavedState.ProfileData

    data class Current(
        override val tokens: SavedState.AuthTokens?,
        override val profileData: SavedState.ProfileData,
    ) : SessionContext()

    data class Previous(
        override val tokens: SavedState.AuthTokens.Authenticated,
        override val profileData: SavedState.ProfileData,
    ) : SessionContext()

    companion object Key : CoroutineContext.Key<SessionContext>
}
