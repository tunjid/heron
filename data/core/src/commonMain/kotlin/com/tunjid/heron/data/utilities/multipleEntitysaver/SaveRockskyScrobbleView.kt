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
            albumArt = scrobbleView.albumArt?.let { ImageUri(it.uri) },
            handle = scrobbleView.handle?.let { ProfileHandle(it.handle) },
            did = ProfileId(scrobbleView.did.did),
            avatar = scrobbleView.avatar?.let { ImageUri(it.uri) },
            trackUri = scrobbleView.trackUri?.let { TrackUri(it.atUri) },
            artistUri = scrobbleView.artistUri?.let { ArtistUri(it.atUri) },
            albumUri = scrobbleView.albumUri?.let { AlbumUri(it.atUri) },
            createdAt = scrobbleView.createdAt,
        ),
    )
}
