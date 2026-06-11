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

package com.tunjid.heron.data.repository.records

import app.rocksky.actor.GetActorAlbumsQueryParams
import app.rocksky.actor.GetActorArtistsQueryParams
import app.rocksky.actor.GetActorScrobblesQueryParams
import app.rocksky.actor.GetActorSongsQueryParams
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.RockskyAlbum
import com.tunjid.heron.data.core.models.RockskyArtist
import com.tunjid.heron.data.core.models.RockskyScrobble
import com.tunjid.heron.data.core.models.RockskyTrack
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.daos.RockskyDao
import com.tunjid.heron.data.database.entities.PopulatedRockSkyAlbumEntity
import com.tunjid.heron.data.database.entities.RockskyArtistEntity
import com.tunjid.heron.data.database.entities.RockskyScrobbleEntity
import com.tunjid.heron.data.database.entities.RockskyTrackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.CursorQueryRefreshTracker
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.albumsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.artistsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.scrobblesIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.tracksIdentity
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn

interface RockskyRecordOperations {

    fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyAlbum>>

    fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyTrack>>

    fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyArtist>>

    fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyScrobble>>
}

@Inject
internal class OfflineFirstRockskyRecordOperations(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val rockskyDao: RockskyDao,
    private val profileLookup: ProfileLookup,
    private val savedStateDataSource: SavedStateDataSource,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val cursorQueryRefreshTracker: CursorQueryRefreshTracker,
) : RockskyRecordOperations {

    override fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyAlbum>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                rockskyDao.albums(
                    profileId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { entities ->
                        entities.map(PopulatedRockSkyAlbumEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorAlbums(
                            GetActorAlbumsQueryParams(
                                did = profileDid,
                                limit = query.data.limit,
                                offset = query.data.offset,
                            ),
                        )
                    },
                    nextCursor = {
                        if (albums.size.toLong() == query.data.limit) "" else null
                    },
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::albumsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) rockskyDao.deleteAlbumsForProfile(profileDid.did)
                            albums.forEach {
                                add(creatorId = ProfileId(profileDid.did), albumView = it)
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyTrack>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                rockskyDao.tracks(
                    profileId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { entities ->
                        entities.map(RockskyTrackEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorSongs(
                            GetActorSongsQueryParams(
                                did = profileDid,
                                limit = query.data.limit,
                                offset = query.data.offset,
                            ),
                        )
                    },
                    nextCursor = {
                        if (tracks.size.toLong() == query.data.limit) "" else null
                    },
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::tracksIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) rockskyDao.deleteTracksForProfile(profileDid.did)
                            tracks.forEach {
                                add(creatorId = ProfileId(profileDid.did), trackView = it)
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyArtist>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                rockskyDao.artists(
                    profileId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { entities ->
                        entities.map(RockskyArtistEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorArtists(
                            GetActorArtistsQueryParams(
                                did = profileDid,
                                limit = query.data.limit,
                                offset = query.data.offset,
                            ),
                        )
                    },
                    nextCursor = {
                        if (artists.size.toLong() == query.data.limit) "" else null
                    },
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::artistsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) rockskyDao.deleteArtistsForProfile(profileDid.did)
                            artists.forEach {
                                add(creatorId = ProfileId(profileDid.did), artistView = it)
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockskyScrobble>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                rockskyDao.scrobbles(
                    profileId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { entities ->
                        entities.map(RockskyScrobbleEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorScrobbles(
                            GetActorScrobblesQueryParams(
                                did = profileDid,
                                limit = query.data.limit,
                                offset = query.data.offset,
                            ),
                        )
                    },
                    nextCursor = {
                        if (scrobbles.size.toLong() == query.data.limit) "" else null
                    },
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::scrobblesIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) rockskyDao.deleteScrobblesForProfile(profileDid.did)
                            scrobbles.forEach {
                                add(creatorId = ProfileId(profileDid.did), scrobbleView = it)
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)
}
