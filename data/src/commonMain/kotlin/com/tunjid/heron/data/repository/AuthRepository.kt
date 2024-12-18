/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.repository

import app.bsky.actor.GetProfileQueryParams
import com.atproto.server.CreateSessionRequest
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.local.models.SessionRequest
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.signedInUserProfileEntity
import com.tunjid.heron.data.utilities.runCatchingCoroutines
import com.tunjid.heron.data.utilities.runCatchingWithIoMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did

interface AuthRepository {
    val isSignedIn: Flow<Boolean>

    suspend fun createSession(request: SessionRequest): Result<Unit>

    suspend fun updateSignedInUser()
}

@Inject
class AuthTokenRepository(
    private val profileDao: ProfileDao,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> =
        savedStateRepository.savedState.map { it.auth != null }

    override suspend fun createSession(
        request: SessionRequest
    ): Result<Unit> = runCatchingWithIoMessage {
        networkService.api.createSession(
            CreateSessionRequest(
                identifier = request.username,
                password = request.password,
            )
        )
    }
        .mapCatching { result ->
            coroutineScope {
                awaitAll(
                    async {
                        updateSignedInUser(result.did)
                    },
                    async {
                        savedStateRepository.updateState {
                            copy(
                                auth = SavedState.AuthTokens(
                                    authProfileId = Id(result.did.did),
                                    auth = result.accessJwt,
                                    refresh = result.refreshJwt,
                                )
                            )
                        }
                    }
                )
            }
        }


    override suspend fun updateSignedInUser() {
        runCatchingCoroutines {
            networkService.api.getSession()
                .maybeResponse()
                ?.did
                ?.let { updateSignedInUser(it) }
        }
    }

    private suspend fun updateSignedInUser(did: Did) {
        runCatchingCoroutines {
            networkService.api.getProfile(GetProfileQueryParams(actor = did))
                .maybeResponse()
                ?.signedInUserProfileEntity()
                ?.let { profileDao.upsertProfiles(listOf(it)) }
        }
    }
}