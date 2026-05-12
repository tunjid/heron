package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.RockskyTrack
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import kotlin.time.Instant

@Entity(
    tableName = "rockskyTracks",
    foreignKeys = [
        ForeignKey(
            entity = RockskyAlbumEntity::class,
            parentColumns = ["uri"],
            childColumns = ["albumUri"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RockskyArtistEntity::class,
            parentColumns = ["uri"],
            childColumns = ["artistUri"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["creatorId"]),
        Index(value = ["albumUri"]),
        Index(value = ["artistUri"]),
        Index(value = ["createdAt"]),
    ],
)
data class RockskyTrackEntity(
    @PrimaryKey
    val uri: TrackUri,
    val cid: TrackId,
    val creatorId: ProfileId,
    val title: String,
    val artist: String,
    val albumArtist: String?,
    val album: String?,
    val albumArt: ImageUri?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Long?,
    val albumUri: AlbumUri?,
    val artistUri: ArtistUri?,
    val createdAt: Instant?,
    val playCount: Long?,
    val uniqueListeners: Long?,
)

fun RockskyTrackEntity.asExternalModel() = RockskyTrack(
    cid = cid,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumArt = albumArt,
    trackNumber = trackNumber,
    discNumber = discNumber,
    duration = duration,
    uri = uri,
    albumUri = albumUri,
    artistUri = artistUri,
    createdAt = createdAt,
    playCount = playCount,
    uniqueListeners = uniqueListeners,
)
