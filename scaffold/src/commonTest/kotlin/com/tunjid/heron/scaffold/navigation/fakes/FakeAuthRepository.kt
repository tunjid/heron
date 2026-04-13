package com.tunjid.heron.scaffold.navigation.fakes

import com.tunjid.heron.data.core.models.OauthUriRequest
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.models.SessionRequest
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository : AuthRepository {

    val signedInState = MutableStateFlow(false)
    val guestState = MutableStateFlow(false)
    val signedInUserState = MutableStateFlow<Profile?>(null)
    val pastSessionsState = MutableStateFlow<List<SessionSummary>>(emptyList())

    override val isSignedIn: Flow<Boolean> = signedInState
    override val isGuest: Flow<Boolean> = guestState
    override val signedInUser: Flow<Profile?> = signedInUserState
    override val pastSessions: Flow<List<SessionSummary>> = pastSessionsState

    override fun isSignedInProfile(id: ProfileId): Flow<Boolean> = flowOf(false)

    override suspend fun oauthRequestUri(request: OauthUriRequest): Result<GenericUri> =
        Result.failure(NotImplementedError())

    override suspend fun createSession(request: SessionRequest): Outcome = Outcome.Success

    override suspend fun switchSession(sessionSummary: SessionSummary): Outcome = Outcome.Success

    override suspend fun signOut() = Unit

    override suspend fun updateSignedInUser(): Outcome = Outcome.Success

    override suspend fun resolveServer(handle: ProfileHandle): Result<Server> =
        Result.failure(NotImplementedError())
}
