package com.tunjid.heron.data.utilities.multipleEntitysaver

import com.tunjid.heron.data.core.types.AlbumId
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.database.entities.RockskyAlbumEntity
import com.tunjid.heron.data.database.entities.RockskyArtistEntity
import com.tunjid.heron.data.database.entities.RockskyTrackEntity

internal fun stubRockskyArtistEntity(
    uri: ArtistUri,
    creatorId: ProfileId,
    name: String,
) = RockskyArtistEntity(
    uri = uri,
    cid = ArtistId(uri.uri),
    creatorId = creatorId,
    name = name,
    picture = null,
    playCount = null,
    uniqueListeners = null,
    tags = null,
)

internal fun stubRockskyAlbumEntity(
    uri: AlbumUri,
    creatorId: ProfileId,
    artistUri: ArtistUri,
    title: String,
    artist: String,
    albumArt: ImageUri?,
) = RockskyAlbumEntity(
    uri = uri,
    cid = AlbumId(uri.uri),
    creatorId = creatorId,
    title = title,
    artist = artist,
    releaseDate = null,
    year = null,
    albumArt = albumArt,
    artistUri = artistUri,
    playCount = null,
    uniqueListeners = null,
)

internal fun stubRockskyTrackEntity(
    uri: TrackUri,
    cid: TrackId,
    creatorId: ProfileId,
    title: String,
    artist: String,
    albumArtist: String?,
    album: String?,
    albumArt: ImageUri?,
    albumUri: AlbumUri?,
    artistUri: ArtistUri?,
) = RockskyTrackEntity(
    uri = uri,
    cid = cid,
    creatorId = creatorId,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumArt = albumArt,
    trackNumber = null,
    discNumber = null,
    duration = null,
    albumUri = albumUri,
    artistUri = artistUri,
    createdAt = null,
    playCount = null,
    uniqueListeners = null,
)
