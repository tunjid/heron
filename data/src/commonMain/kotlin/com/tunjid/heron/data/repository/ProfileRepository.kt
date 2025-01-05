package com.tunjid.heron.data.repository

import app.bsky.actor.GetProfileQueryParams
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
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

    fun profileRelationship(
        profileId: Id,
    ): Flow<ProfileRelationship>
}

class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
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


    override fun profileRelationship(
        profileId: Id,
    ): Flow<ProfileRelationship> =
        signedInProfileId()
            .flatMapLatest {
                profileDao.relationships(
                    profileId = it.id,
                    otherProfileId = profileId.id,
                )
            }
            .mapNotNull { it?.asExternalModel() }

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
                profileDao.upsertProfiles(
                    listOf(
                        ProfileEntity(
                            did = Id(response.did.did),
                            handle = Id(response.handle.handle),
                            displayName = response.displayName,
                            description = response.description,
                            avatar = response.avatar?.uri?.let(::Uri),
                            banner = response.banner?.uri?.let(::Uri),
                            followersCount = response.followersCount,
                            followsCount = response.followsCount,
                            postsCount = response.postsCount,
                            joinedViaStarterPack = response
                                .joinedViaStarterPack?.cid?.cid?.let(::Id),
                            indexedAt = response.indexedAt,
                            createdAt = response.createdAt,
                        )
                    )
                )
            }

    }
}