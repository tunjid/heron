package com.tunjid.heron.fakes

import com.tunjid.heron.data.repository.SavedState
import kotlinx.serialization.Serializable

@Serializable
data class GuestSavedState(
    override val auth: SavedState.AuthTokens.Guest,
    override val navigation: SavedState.Navigation,
    override val signedInProfileData: ProfileData? = null,
) : SavedState()

@Serializable
data class BearerSavedState(
    override val auth: SavedState.AuthTokens.Authenticated.Bearer,
    override val navigation: SavedState.Navigation,
    override val signedInProfileData: ProfileData?,
) : SavedState()

@Serializable
data class DPoPSavedState(
    override val auth: SavedState.AuthTokens.Authenticated.DPoP,
    override val navigation: SavedState.Navigation,
    override val signedInProfileData: ProfileData? = null,
) : SavedState()
