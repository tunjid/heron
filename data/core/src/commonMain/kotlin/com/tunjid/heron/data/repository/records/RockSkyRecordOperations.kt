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

import app.rocksky.actor.AlbumView
import app.rocksky.actor.ArtistView
import app.rocksky.actor.GetActorAlbumsQueryParams
import app.rocksky.actor.GetActorArtistsQueryParams
import app.rocksky.actor.GetActorScrobblesQueryParams
import app.rocksky.actor.GetActorSongsQueryParams
import app.rocksky.actor.ScrobbleView
import app.rocksky.actor.TrackView
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.models.RockSkyScrobble
import com.tunjid.heron.data.core.models.RockSkyTrack
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.asExternalModel
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import sh.christian.ozone.api.response.AtpResponse

interface RockSkyRecordOperations {

    fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyAlbum>>

    fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyTrack>>

    fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyArtist>>

    fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyScrobble>>
}

// TODO: persist via RockSkyDao + per-user cross-ref tables for true offline-first reads.
// For now these are network-only and route through the heron appview proxy
// (see HeronProxyPaths in SessionManager).
internal class OfflineFirstRockSkyRecordOperations @Inject constructor(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val profileLookup: ProfileLookup,
    private val savedStateDataSource: SavedStateDataSource,
) : RockSkyRecordOperations {

    override fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyAlbum>> = paged(
        query = query,
        cursor = cursor,
        request = { profileDid ->
            getActorAlbums(
                GetActorAlbumsQueryParams(
                    did = profileDid,
                    limit = query.data.limit,
                    offset = query.data.offset,
                ),
            )
        },
        items = { albums.map(AlbumView::asExternalModel) },
    )

    override fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyTrack>> = paged(
        query = query,
        cursor = cursor,
        request = { profileDid ->
            getActorSongs(
                GetActorSongsQueryParams(
                    did = profileDid,
                    limit = query.data.limit,
                    offset = query.data.offset,
                ),
            )
        },
        items = { tracks.map(TrackView::asExternalModel) },
    )

    override fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyArtist>> = paged(
        query = query,
        cursor = cursor,
        request = { profileDid ->
            getActorArtists(
                GetActorArtistsQueryParams(
                    did = profileDid,
                    limit = query.data.limit,
                    offset = query.data.offset,
                ),
            )
        },
        items = { artists.map(ArtistView::asExternalModel) },
    )

    override fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyScrobble>> = paged(
        query = query,
        cursor = cursor,
        request = { profileDid ->
            getActorScrobbles(
                GetActorScrobblesQueryParams(
                    did = profileDid,
                    limit = query.data.limit,
                    offset = query.data.offset,
                ),
            )
        },
        items = { scrobbles.map(ScrobbleView::asExternalModel) },
    )

    /**
     * Runs an offset-paginated XRPC request through the heron appview proxy and emits
     * a [CursorList] for the page. The actual offset comes from [ProfilesQuery.data]
     * (page * limit); the [Cursor] state machine only gates the fetch lifecycle.
     *
     * Emissions:
     * - On entry: `(emptyList(), Cursor.Pending)` so the consumer can render a loader.
     * - On a full page: `(items, Cursor.Next(""))` — the cursor value is unused, the
     *   consumer advances by bumping `query.data.page`.
     * - On a partial page: `(items, Cursor.Final)`.
     * - On request failure: nothing further; the flow ends in the Pending state.
     */
    private fun <Response : Any, External> paged(
        query: ProfilesQuery,
        cursor: Cursor,
        request: suspend BlueskyApi.(profileDid: sh.christian.ozone.api.Did) -> AtpResponse<Response>,
        items: Response.() -> List<External>,
    ): Flow<CursorList<External>> = savedStateDataSource.singleSessionFlow {
        val profileDid = profileLookup.lookupProfileDid(
            profileId = query.profileId,
        ) ?: return@singleSessionFlow emptyFlow()

        flow {
            if (cursor is Cursor.Final) return@flow
            emit(CursorList(emptyList(), Cursor.Pending))
            if (cursor is Cursor.Pending) return@flow

            val pageItems = networkService.runCatchingWithMonitoredNetworkRetry {
                request(profileDid)
            }
                .getOrNull()
                ?.items()
                ?: return@flow

            val nextCursor =
                if (pageItems.size.toLong() == query.data.limit) Cursor.Next("")
                else Cursor.Final
            emit(CursorList(pageItems, nextCursor))
        }
    }.flowOn(ioDispatcher)
}
