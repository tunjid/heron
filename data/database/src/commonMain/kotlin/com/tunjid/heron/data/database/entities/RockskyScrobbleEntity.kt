package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.RockskyScrobble
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import kotlin.time.Instant

@Entity(
    tableName = "rockskyScrobbles",
    foreignKeys =
        [
            ForeignKey(
                entity = ProfileEntity::class,
                parentColumns = ["did"],
                childColumns = ["did"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = RockskyTrackEntity::class,
                parentColumns = ["uri"],
                childColumns = ["trackUri"],
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
            ForeignKey(
                entity = RockskyAlbumEntity::class,
                parentColumns = ["uri"],
                childColumns = ["albumUri"],
                onDelete = ForeignKey.SET_NULL,
                onUpdate = ForeignKey.CASCADE,
            ),
        ],
    indices =
        [
            Index(value = ["uri"]),
            Index(value = ["did"]),
            Index(value = ["creatorId"]),
            Index(value = ["trackUri"]),
            Index(value = ["artistUri"]),
            Index(value = ["albumUri"]),
            Index(value = ["createdAt"]),
        ],
)
data class RockskyScrobbleEntity(
    @PrimaryKey val uri: ScrobbleUri,
    val cid: ScrobbleId,
    val creatorId: ProfileId,
    val trackId: TrackId,
    val title: String,
    val artist: String,
    val albumArtist: String?,
    val album: String?,
    val albumArt: ImageUri?,
    val handle: ProfileHandle?,
    val did: ProfileId,
    val avatar: ImageUri?,
    val trackUri: TrackUri?,
    val artistUri: ArtistUri?,
    val albumUri: AlbumUri?,
    val createdAt: Instant,
)

fun RockskyScrobbleEntity.asExternalModel() =
    RockskyScrobble(
        cid = cid,
        trackId = trackId,
        title = title,
        artist = artist,
        albumArtist = albumArtist,
        album = album,
        albumArt = albumArt,
        handle = handle,
        did = did,
        avatar = avatar,
        uri = uri,
        trackUri = trackUri,
        artistUri = artistUri,
        albumUri = albumUri,
        createdAt = createdAt,
    )
