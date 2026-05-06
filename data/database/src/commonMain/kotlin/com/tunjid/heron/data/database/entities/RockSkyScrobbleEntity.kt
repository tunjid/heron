package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.RockSkyScrobble
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
    tableName = "rockSkyScrobbles",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["did"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RockSkyTrackEntity::class,
            parentColumns = ["uri"],
            childColumns = ["trackUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RockSkyArtistEntity::class,
            parentColumns = ["uri"],
            childColumns = ["artistUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RockSkyAlbumEntity::class,
            parentColumns = ["uri"],
            childColumns = ["albumUri"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["did"]),
        Index(value = ["trackUri"]),
        Index(value = ["artistUri"]),
        Index(value = ["albumUri"]),
        Index(value = ["createdAt"]),
    ],
)
data class RockSkyScrobbleEntity(
    @PrimaryKey
    val uri: ScrobbleUri,
    val cid: ScrobbleId,
    val trackId: TrackId,
    val title: String,
    val artist: String,
    val albumArtist: String?,
    val album: String?,
    val albumArt: ImageUri?,
    val handle: ProfileHandle?,
    val did: ProfileId?,
    val avatar: ImageUri?,
    val trackUri: TrackUri?,
    val artistUri: ArtistUri?,
    val albumUri: AlbumUri?,
    val createdAt: Instant,
)

data class PopulatedRockSkyScrobbleEntity(
    @Embedded
    val entity: RockSkyScrobbleEntity,
    @Embedded(prefix = "profile_")
    val profile: ProfileEntity?,
)

fun PopulatedRockSkyScrobbleEntity.asExternalModel() = RockSkyScrobble(
    cid = entity.cid,
    trackId = entity.trackId,
    title = entity.title,
    artist = entity.artist,
    albumArtist = entity.albumArtist,
    album = entity.album,
    albumArt = entity.albumArt,
    handle = entity.handle,
    did = entity.did,
    avatar = entity.avatar,
    uri = entity.uri,
    trackUri = entity.trackUri,
    artistUri = entity.artistUri,
    albumUri = entity.albumUri,
    createdAt = entity.createdAt,
)
