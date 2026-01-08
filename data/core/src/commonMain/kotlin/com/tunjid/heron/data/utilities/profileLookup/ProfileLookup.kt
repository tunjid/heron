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

package com.tunjid.heron.data.utilities.profileLookup

import app.bsky.actor.GetProfileQueryParams
import app.bsky.actor.ProfileView
import com.atproto.identity.ResolveHandleQueryParams
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.profile
import com.tunjid.heron.data.network.models.profileViewerStateEntities
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle
import sh.christian.ozone.api.response.AtpResponse

internal interface ProfileLookup {
    fun <NetworkResponse : Any> profilesWithViewerState(
        signedInProfileId: ProfileId?,
        responseFetcher: suspend BlueskyApi.() -> AtpResponse<NetworkResponse>,
        responseProfileViews: NetworkResponse.() -> List<ProfileView>,
        responseCursor: NetworkResponse.() -> String?,
    ): Flow<CursorList<ProfileWithViewerState>>
}

internal class OfflineProfileLookup @Inject constructor(
    private val profileDao: ProfileDao,
    private val networkService: NetworkService,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
) : ProfileLookup {
    override fun <NetworkResponse : Any> profilesWithViewerState(
        signedInProfileId: ProfileId?,
        responseFetcher: suspend BlueskyApi.() -> AtpResponse<NetworkResponse>,
        responseProfileViews: NetworkResponse.() -> List<ProfileView>,
        responseCursor: NetworkResponse.() -> String?,
    ): Flow<CursorList<ProfileWithViewerState>> = flow {
        val response = networkService.runCatchingWithMonitoredNetworkRetry(
            block = responseFetcher,
        ).getOrNull()
            ?: return@flow

        val profileViews = response.responseProfileViews()

        val nextCursor = response.responseCursor()
            ?.let(Cursor::Next)
            ?: Cursor.Pending

        // Emit network results immediately for minimal latency
        emit(
            CursorList(
                items = profileViews.toProfileWithViewerStates(
                    signedInProfileId = signedInProfileId,
                    profileMapper = ProfileView::profile,
                    profileViewerStateEntities = ProfileView::profileViewerStateEntities,
                ),
                nextCursor = nextCursor,
            ),
        )

        multipleEntitySaverProvider.saveInTransaction {
            profileViews
                .forEach { profileView ->
                    add(
                        viewingProfileId = signedInProfileId,
                        profileView = profileView,
                    )
                }
        }

        emitAll(
            profileViews.observeProfileWithViewerStates(
                profileDao = profileDao,
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileView::profile,
                idMapper = { did.did.let(::ProfileId) },
            )
                .map { profileWithViewerStates ->
                    CursorList(
                        items = profileWithViewerStates,
                        nextCursor = nextCursor,
                    )
                },
        )
    }
}

internal suspend fun lookupProfileDid(
    profileId: Id.Profile,
    profileDao: ProfileDao,
    networkService: NetworkService,
): Did? {
    val profileHandleOrId = profileId.id
    return when {
        Did.Regex.matches(profileHandleOrId) -> Did(profileHandleOrId)
        Handle.Regex.matches(profileHandleOrId) -> profileDao.profiles(
            ids = listOf(ProfileHandleOrId(profileHandleOrId)),
        )
            .first()
            .takeIf(List<PopulatedProfileEntity>::isNotEmpty)
            ?.first()
            ?.entity
            ?.did
            ?.id
            ?.let(::Did)
            ?: networkService.runCatchingWithMonitoredNetworkRetry {
                resolveHandle(
                    params = ResolveHandleQueryParams(
                        Handle(profileHandleOrId),
                    ),
                )
            }
                .getOrNull()
                ?.did

        else -> null
    }
}

internal suspend fun resolveLinks(
    profileDao: ProfileDao,
    networkService: NetworkService,
    links: List<Link>,
): List<Link> =
    coroutineScope {
        links.map { link ->
            async {
                when (val target = link.target) {
                    is LinkTarget.ExternalLink -> link
                    is LinkTarget.UserDidMention -> link
                    is LinkTarget.Hashtag -> link
                    is LinkTarget.UserHandleMention -> {
                        lookupProfileDid(
                            profileId = target.handle,
                            profileDao = profileDao,
                            networkService = networkService,
                        )?.let { did ->
                            link.copy(
                                target = LinkTarget.UserDidMention(did.did.let(::ProfileId)),
                            )
                        }
                    }
                }
            }
        }.awaitAll()
    }.filterNotNull()

internal suspend fun refreshProfile(
    profileId: Id.Profile,
    profileDao: ProfileDao,
    networkService: NetworkService,
    multipleEntitySaverProvider: MultipleEntitySaverProvider,
    savedStateDataSource: SavedStateDataSource,
) = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
    val profileDid = lookupProfileDid(
        profileId = profileId,
        profileDao = profileDao,
        networkService = networkService,
    ) ?: return@inCurrentProfileSession

    networkService.runCatchingWithMonitoredNetworkRetry {
        getProfile(GetProfileQueryParams(actor = profileDid))
    }
        .getOrNull()
        ?.let { response ->
            multipleEntitySaverProvider.saveInTransaction {
                add(
                    viewingProfileId = signedInProfileId,
                    profileView = response,
                )
            }
        }
}

private inline fun <ProfileViewType> List<ProfileViewType>.toProfileWithViewerStates(
    signedInProfileId: ProfileId?,
    crossinline profileMapper: ProfileViewType.() -> Profile,
    crossinline profileViewerStateEntities: ProfileViewType.(ProfileId) -> List<ProfileViewerStateEntity>,
): List<ProfileWithViewerState> = map { profileView ->
    ProfileWithViewerState(
        profile = profileView.profileMapper(),
        viewerState =
        if (signedInProfileId == null) null
        else profileView.profileViewerStateEntities(
            signedInProfileId,
        )
            .first()
            .asExternalModel(),
    )
}

private inline fun <ProfileViewType> List<ProfileViewType>.observeProfileWithViewerStates(
    profileDao: ProfileDao,
    signedInProfileId: ProfileId?,
    crossinline profileMapper: ProfileViewType.() -> Profile,
    crossinline idMapper: ProfileViewType.() -> ProfileId,
): Flow<List<ProfileWithViewerState>> {
    val profileViews = this
    return when (signedInProfileId) {
        null -> flowOf(
            map { profileView ->
                ProfileWithViewerState(
                    profile = profileView.profileMapper(),
                    viewerState = null,
                )
            },
        )

        else -> profileDao.viewerState(
            profileId = signedInProfileId.id,
            otherProfileIds = mapTo(
                destination = mutableSetOf(),
                transform = idMapper,
            ),
        )
            .distinctUntilChanged()
            .map { viewerStates ->
                val profileIdsToViewerStates = viewerStates.associateBy(
                    ProfileViewerStateEntity::otherProfileId,
                )

                profileViews.map { profileViewBasic ->
                    val profile = profileViewBasic.profileMapper()
                    ProfileWithViewerState(
                        profile = profile,
                        viewerState = profileIdsToViewerStates[profile.did]?.asExternalModel(),
                    )
                }
            }
    }
}
