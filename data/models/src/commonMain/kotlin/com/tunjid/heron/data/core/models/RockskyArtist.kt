package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import kotlinx.serialization.Serializable

@Serializable
data class RockskyArtist(
    val cid: ArtistId,
    val name: String,
    val picture: ImageUri?,
    val uri: ArtistUri,
    val playCount: Long? = null,
    val uniqueListeners: Long? = null,
    val tags: List<String>? = null,
) : Record {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )
}
