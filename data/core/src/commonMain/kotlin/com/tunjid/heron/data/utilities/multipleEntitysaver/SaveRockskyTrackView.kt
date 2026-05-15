package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.rocksky.actor.TrackView
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.database.entities.RockskyTrackEntity
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    creatorId: ProfileId,
    trackView: TrackView,
) {
    add(stubProfileEntity(Did(creatorId.id)))

    val artistUri = trackView.artistUri?.let { ArtistUri(it.atUri) }
    val albumArt = trackView.albumArt?.let { ImageUri(it.uri) }
    // Album requires a non-null artistUri FK; if we don't have one, drop the
    // track's albumUri rather than stubbing an album we can't satisfy.
    val albumUri = trackView.albumUri
        ?.takeIf { artistUri != null }
        ?.let { AlbumUri(it.atUri) }

    if (artistUri != null) {
        add(
            stubRockskyArtistEntity(
                uri = artistUri,
                creatorId = creatorId,
                name = trackView.artist,
            ),
        )
    }

    if (albumUri != null && artistUri != null) {
        add(
            stubRockskyAlbumEntity(
                uri = albumUri,
                creatorId = creatorId,
                artistUri = artistUri,
                title = trackView.album ?: trackView.title,
                artist = trackView.albumArtist ?: trackView.artist,
                albumArt = albumArt,
            ),
        )
    }

    add(
        RockskyTrackEntity(
            uri = TrackUri(trackView.uri.atUri),
            cid = TrackId(trackView.id),
            creatorId = creatorId,
            title = trackView.title,
            artist = trackView.artist,
            albumArtist = trackView.albumArtist,
            album = trackView.album,
            albumArt = albumArt,
            trackNumber = trackView.trackNumber?.toInt(),
            discNumber = trackView.discNumber?.toInt(),
            duration = trackView.duration,
            albumUri = albumUri,
            artistUri = artistUri,
            createdAt = trackView.createdAt,
            playCount = trackView.playCount,
            uniqueListeners = trackView.uniqueListeners,
        ),
    )
}
