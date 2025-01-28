/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.repository

import app.bsky.actor.GetProfileQueryParams
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.withRefresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did

interface ProfileRepository {

    fun signedInProfile(): Flow<Profile>

    fun profile(profileId: Id): Flow<Profile>

    fun profileRelationships(
        profileIds: Set<Id>,
    ): Flow<List<ProfileRelationship>>
}

class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : ProfileRepository {

    override fun signedInProfile(): Flow<Profile> =
        signedInProfileId()
            .flatMapLatest { profileDao.profiles(listOf(it)) }
            .mapNotNull { it.firstOrNull()?.asExternalModel() }

    override fun profile(
        profileId: Id,
    ): Flow<Profile> =
        profileDao.profiles(listOf(profileId))
            .map { it.firstOrNull()?.asExternalModel() }
            .filterNotNull()
            .withRefresh { fetchProfile(profileId) }
            .distinctUntilChanged()

    override fun profileRelationships(
        profileIds: Set<Id>,
    ): Flow<List<ProfileRelationship>> =
        signedInProfileId()
            .flatMapLatest {
                profileDao.relationships(
                    profileId = it.id,
                    otherProfileIds = profileIds,
                )
            }
            .map { relationshipsEntities ->
                relationshipsEntities.map(ProfileProfileRelationshipsEntity::asExternalModel)
            }
            .distinctUntilChanged()

    private fun signedInProfileId() = savedStateRepository.savedState
        .mapNotNull { it.auth?.authProfileId }
        .distinctUntilChanged()

    private suspend fun fetchProfile(profileId: Id) {
        runCatchingWithNetworkRetry {
            networkService.api.getProfile(
                GetProfileQueryParams(actor = Did(profileId.id))
            )
        }
            .getOrNull()
            ?.let { response ->
                multipleEntitySaverProvider.saveInTransaction {
                    add(
                        viewingProfileId = savedStateRepository.signedInProfileId,
                        profileView = response,
                    )
                }
            }
    }
}