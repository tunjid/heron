package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.rocksky.actor.ArtistView
import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.RockskyArtistEntity

internal fun MultipleEntitySaver.add(
    creatorId: ProfileId,
    artistView: ArtistView,
) {
    add(
        RockskyArtistEntity(
            uri = ArtistUri(artistView.uri.atUri),
            cid = ArtistId(artistView.id),
            creatorId = creatorId,
            name = artistView.name,
            picture = artistView.picture?.let { ImageUri(it.uri) },
            playCount = artistView.playCount,
            uniqueListeners = artistView.uniqueListeners,
            tags = artistView.tags?.joinToString(","),
        ),
    )
}
