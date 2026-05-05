package com.tunjid.heron.data.repository.records

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.models.RockSkyScrobble
import com.tunjid.heron.data.core.models.RockSkyTrack
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

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

internal class OfflineFirstRockSkyRecordOperations @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
) : RockSkyRecordOperations {

    override fun albums(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyAlbum>> {
        TODO("Not yet implemented")
    }

    override fun tracks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyTrack>> {
        TODO("Not yet implemented")
    }

    override fun artists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyArtist>> {
        TODO("Not yet implemented")
    }

    override fun scrobbles(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<RockSkyScrobble>> {
        TODO("Not yet implemented")
    }
}
