package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.rocksky.actor.AlbumView
import com.tunjid.heron.data.core.types.AlbumId
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.RockskyAlbumEntity

internal fun MultipleEntitySaver.add(
    creatorId: ProfileId,
    albumView: AlbumView,
) {
    add(
        RockskyAlbumEntity(
            uri = AlbumUri(albumView.uri.atUri),
            cid = AlbumId(albumView.id),
            creatorId = creatorId,
            title = albumView.title,
            artist = albumView.artist,
            releaseDate = albumView.releaseDate,
            year = albumView.year?.toInt(),
            albumArt = albumView.albumArt?.let { ImageUri(it.uri) },
            artistUri = ArtistUri(albumView.artistUri.atUri),
            playCount = albumView.playCount,
            uniqueListeners = albumView.uniqueListeners,
            appleMusicLink = albumView.appleMusicLink?.uri,
            spotifyLink = albumView.spotifyLink?.uri,
            tidalLink = albumView.tidalLink?.uri,
            youtubeLink = albumView.youtubeLink?.uri,
        ),
    )
}
