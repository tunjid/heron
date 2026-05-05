package com.tunjid.heron.data.repository.records

import com.tunjid.heron.data.core.models.Album
import com.tunjid.heron.data.core.models.Artist
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Scrobble
import com.tunjid.heron.data.core.models.Track
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

interface RockSkyRecordOperations {

    fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Album>>

    fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Track>>

    fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Artist>>

    fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Scrobble>>
}

internal class OfflineFirstRockSkyRecordOperations @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
) : RockSkyRecordOperations {

    override fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Album>> {
        TODO("Not yet implemented")
    }

    override fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Track>> {
        TODO("Not yet implemented")
    }

    override fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Artist>> {
        TODO("Not yet implemented")
    }

    override fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Scrobble>> {
        TODO("Not yet implemented")
    }
}
