package com.tunjid.heron.data.repository

import app.bsky.actor.GetProfileQueryParams
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.runCatchingCoroutines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

interface ProfileRepository {
    fun profile(profileId: Id): Flow<Profile>
}

class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val networkService: NetworkService,
) : ProfileRepository {

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