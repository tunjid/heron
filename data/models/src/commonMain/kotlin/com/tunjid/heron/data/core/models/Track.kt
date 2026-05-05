package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: TrackId,
    val title: String,
    val artist: String,
    val albumArtist: String?,
    val album: String?,
    val albumArt: ImageUri?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Long?,
    val uri: TrackUri,
    val albumUri: AlbumUri?,
    val artistUri: ArtistUri?,
    val createdAt: Instant?,
    val playCount: Long? = null,
    val uniqueListeners: Long? = null,
) : Record {

    override val reference: Record.Reference =
        Record.Reference(
            id = id,
            uri = uri,
        )
}
