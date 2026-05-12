package com.tunjid.heron.data.core.models

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
import kotlinx.serialization.Serializable

@Serializable
data class RockskyScrobble(
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
    val uri: ScrobbleUri,
    val trackUri: TrackUri?,
    val artistUri: ArtistUri?,
    val albumUri: AlbumUri?,
    val createdAt: Instant,
) : Record {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )
}
