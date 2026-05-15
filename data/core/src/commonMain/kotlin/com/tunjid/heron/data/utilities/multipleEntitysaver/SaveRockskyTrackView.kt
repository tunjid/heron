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
    add(
        RockskyTrackEntity(
            uri = TrackUri(trackView.uri.atUri),
            cid = TrackId(trackView.id),
            creatorId = creatorId,
            title = trackView.title,
            artist = trackView.artist,
            albumArtist = trackView.albumArtist,
            album = trackView.album,
            albumArt = trackView.albumArt?.let { ImageUri(it.uri) },
            trackNumber = trackView.trackNumber?.toInt(),
            discNumber = trackView.discNumber?.toInt(),
            duration = trackView.duration,
            albumUri = trackView.albumUri?.let { AlbumUri(it.atUri) },
            artistUri = trackView.artistUri?.let { ArtistUri(it.atUri) },
            createdAt = trackView.createdAt,
            playCount = trackView.playCount,
            uniqueListeners = trackView.uniqueListeners,
        ),
    )
}
