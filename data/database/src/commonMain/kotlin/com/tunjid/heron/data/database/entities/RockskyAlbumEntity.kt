package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.RockskyAlbum
import com.tunjid.heron.data.core.types.AlbumId
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId

@Entity(
    tableName = "rockskyAlbums",
    foreignKeys =
        [
            ForeignKey(
                entity = RockskyArtistEntity::class,
                parentColumns = ["uri"],
                childColumns = ["artistUri"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE,
            )
        ],
    indices =
        [
            Index(value = ["uri"]),
            Index(value = ["artistUri"]),
            Index(value = ["creatorId"]),
        ],
)
data class RockskyAlbumEntity(
    @PrimaryKey val uri: AlbumUri,
    val cid: AlbumId,
    val creatorId: ProfileId,
    val title: String,
    val artist: String,
    val releaseDate: String?,
    val year: Int?,
    val albumArt: ImageUri?,
    val artistUri: ArtistUri,
    val playCount: Long?,
    val uniqueListeners: Long?,
    val appleMusicLink: String? = null,
    val spotifyLink: String? = null,
    val tidalLink: String? = null,
    val youtubeLink: String? = null,
)

data class PopulatedRockSkyAlbumEntity(
    @Embedded val entity: RockskyAlbumEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "albumUri",
    )
    val tracks: List<RockskyTrackEntity>,
)

fun PopulatedRockSkyAlbumEntity.asExternalModel() =
    RockskyAlbum(
        cid = entity.cid,
        uri = entity.uri,
        title = entity.title,
        artist = entity.artist,
        releaseDate = entity.releaseDate,
        year = entity.year,
        albumArt = entity.albumArt,
        artistUri = entity.artistUri,
        playCount = entity.playCount,
        uniqueListeners = entity.uniqueListeners,
        tracks = tracks.map(RockskyTrackEntity::asExternalModel),
        appleMusicLink = entity.appleMusicLink,
        spotifyLink = entity.spotifyLink,
        tidalLink = entity.tidalLink,
        youtubeLink = entity.youtubeLink,
    )
