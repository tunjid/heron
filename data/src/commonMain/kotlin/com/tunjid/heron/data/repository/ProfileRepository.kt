package com.tunjid.heron.data.repository

import app.bsky.actor.GetProfileQueryParams
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.runCatchingCoroutines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

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
        profileId: Id
    ): Flow<Profile> = merge(
        flow {
            fetchProfile(profileId)
        },
        profileDao.profiles(listOf(profileId))
            .map { it.firstOrNull()?.asExternalModel() }
            .filterNotNull()
    )

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
            .map(ProfileProfileRelationshipsEntity::asExternalModel)

    private fun signedInProfileId() = savedStateRepository.savedState
        .mapNotNull { it.auth?.authProfileId }
        .distinctUntilChanged()

    private suspend fun fetchProfile(profileId: Id) {
        runCatchingCoroutines {
            val fetchedProfileResponse = networkService.api.getProfile(
                GetProfileQueryParams(actor = Did(profileId.id))
            )
            when (fetchedProfileResponse) {
                is AtpResponse.Failure -> {
                    // TODO Exponential backoff / network monitoring
                }

                is AtpResponse.Success -> {
                    fetchedProfileResponse.response
                    profileDao.upsertProfiles(
                        listOf(
                            ProfileEntity(
                                did = Id(fetchedProfileResponse.response.did.did),
                                handle = Id(fetchedProfileResponse.response.handle.handle),
                                displayName = fetchedProfileResponse.response.displayName,
                                description = fetchedProfileResponse.response.description,
                                avatar = fetchedProfileResponse.response.avatar?.uri?.let(::Uri),
                                banner = fetchedProfileResponse.response.banner?.uri?.let(::Uri),
                                followersCount = fetchedProfileResponse.response.followersCount,
                                followsCount = fetchedProfileResponse.response.followsCount,
                                postsCount = fetchedProfileResponse.response.postsCount,
                                joinedViaStarterPack = fetchedProfileResponse.response
                                    .joinedViaStarterPack?.cid?.cid?.let(::Id),
                                indexedAt = fetchedProfileResponse.response.indexedAt,
                                createdAt = fetchedProfileResponse.response.createdAt,
                            )
                        )
                    )
                }
            }
        }
    }
}