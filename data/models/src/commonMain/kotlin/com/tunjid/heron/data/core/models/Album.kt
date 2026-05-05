package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.AlbumId
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val cid: AlbumId,
    val uri: AlbumUri,
    val title: String,
    val artist: String,
    val releaseDate: String?,
    val year: Int?,
    val albumArt: ImageUri?,
    val artistUri: ArtistUri?,
    val playCount: Long?,
    val uniqueListeners: Long?,
    val tracks: List<Track> = emptyList(),
    val appleMusicLink: String? = null,
    val spotifyLink: String? = null,
    val tidalLink: String? = null,
    val youtubeLink: String? = null,
) : Record {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )
}
