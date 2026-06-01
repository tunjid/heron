package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.rocksky.actor.ScrobbleView
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.database.entities.RockskyScrobbleEntity
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    creatorId: ProfileId,
    scrobbleView: ScrobbleView,
) {
    add(stubProfileEntity(Did(creatorId.id)))

    val artistUri = scrobbleView.artistUri?.let { ArtistUri(it.atUri) }
    val trackUri = scrobbleView.trackUri?.let { TrackUri(it.atUri) }
    val albumArt = scrobbleView.albumArt?.let { ImageUri(it.uri) }
    // Album requires a non-null artistUri FK; if we don't have one, drop the
    // scrobble's albumUri rather than stubbing an album we can't satisfy.
    val albumUri = scrobbleView.albumUri?.takeIf { artistUri != null }?.let { AlbumUri(it.atUri) }

    if (artistUri != null) {
        add(
            stubRockskyArtistEntity(
                uri = artistUri,
                creatorId = creatorId,
                name = scrobbleView.artist,
            )
        )
    }

    if (albumUri != null && artistUri != null) {
        add(
            stubRockskyAlbumEntity(
                uri = albumUri,
                creatorId = creatorId,
                artistUri = artistUri,
                title = scrobbleView.album ?: scrobbleView.title,
                artist = scrobbleView.albumArtist ?: scrobbleView.artist,
                albumArt = albumArt,
            )
        )
    }

    if (trackUri != null) {
        add(
            stubRockskyTrackEntity(
                uri = trackUri,
                cid = TrackId(scrobbleView.trackId),
                creatorId = creatorId,
                title = scrobbleView.title,
                artist = scrobbleView.artist,
                albumArtist = scrobbleView.albumArtist,
                album = scrobbleView.album,
                albumArt = albumArt,
                albumUri = albumUri,
                artistUri = artistUri,
            )
        )
    }

    add(
        RockskyScrobbleEntity(
            uri = ScrobbleUri(scrobbleView.uri.atUri),
            cid = ScrobbleId(scrobbleView.id),
            trackId = TrackId(scrobbleView.trackId),
            creatorId = creatorId,
            title = scrobbleView.title,
            artist = scrobbleView.artist,
            albumArtist = scrobbleView.albumArtist,
            album = scrobbleView.album,
            albumArt = albumArt,
            handle = scrobbleView.handle?.let { ProfileHandle(it.handle) },
            did = ProfileId(scrobbleView.did.did),
            avatar = scrobbleView.avatar?.let { ImageUri(it.uri) },
            trackUri = trackUri,
            artistUri = artistUri,
            albumUri = albumUri,
            createdAt = scrobbleView.createdAt,
        )
    )
}
