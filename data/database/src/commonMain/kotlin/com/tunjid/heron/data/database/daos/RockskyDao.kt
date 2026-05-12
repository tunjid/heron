package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.PopulatedRockSkyAlbumEntity
import com.tunjid.heron.data.database.entities.RockskyAlbumEntity
import com.tunjid.heron.data.database.entities.RockskyArtistEntity
import com.tunjid.heron.data.database.entities.RockskyScrobbleEntity
import com.tunjid.heron.data.database.entities.RockskyTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RockskyDao {

    @Transaction
    @Upsert
    suspend fun upsertAlbums(
        entities: List<RockskyAlbumEntity>,
    )

    @Transaction
    @Upsert
    suspend fun upsertArtists(
        entities: List<RockskyArtistEntity>,
    )

    @Transaction
    @Upsert
    suspend fun upsertTracks(
        entities: List<RockskyTrackEntity>,
    )

    @Transaction
    @Upsert
    suspend fun upsertScrobbles(
        entities: List<RockskyScrobbleEntity>,
    )

    @Transaction
    @Query(
        """
        SELECT * FROM rockskyAlbums
        WHERE creatorId = :profileId
        LIMIT :limit OFFSET :offset
    """,
    )
    fun albums(
        profileId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedRockSkyAlbumEntity>>

    @Query(
        """
        SELECT * FROM rockskyTracks
        WHERE creatorId = :profileId
        LIMIT :limit OFFSET :offset
    """,
    )
    fun tracks(
        profileId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<RockskyTrackEntity>>

    @Query(
        """
        SELECT * FROM rockskyArtists
        WHERE creatorId = :profileId
        LIMIT :limit OFFSET :offset
    """,
    )
    fun artists(
        profileId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<RockskyArtistEntity>>

    @Query(
        """
        SELECT * FROM rockskyScrobbles
        WHERE creatorId = :profileId
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    fun scrobbles(
        profileId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<RockskyScrobbleEntity>>

    @Query("DELETE FROM rockskyAlbums WHERE creatorId = :profileId")
    suspend fun deleteAlbumsForProfile(profileId: String)

    @Query("DELETE FROM rockskyTracks WHERE creatorId = :profileId")
    suspend fun deleteTracksForProfile(profileId: String)

    @Query("DELETE FROM rockskyArtists WHERE creatorId = :profileId")
    suspend fun deleteArtistsForProfile(profileId: String)

    @Query("DELETE FROM rockskyScrobbles WHERE creatorId = :profileId")
    suspend fun deleteScrobblesForProfile(profileId: String)
}
