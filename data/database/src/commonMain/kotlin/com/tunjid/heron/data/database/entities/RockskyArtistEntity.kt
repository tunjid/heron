package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.RockskyArtist
import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId

@Entity(
    tableName = "rockskyArtists",
    indices = [
        Index(value = ["uri"]),
        Index(value = ["creatorId"]),
    ],
)
data class RockskyArtistEntity(
    @PrimaryKey
    val uri: ArtistUri,
    val cid: ArtistId,
    val creatorId: ProfileId,
    val name: String,
    val picture: ImageUri?,
    val playCount: Long?,
    val uniqueListeners: Long?,
    val tags: String?,
)

fun RockskyArtistEntity.asExternalModel() = RockskyArtist(
    cid = cid,
    name = name,
    picture = picture,
    uri = uri,
    playCount = playCount,
    uniqueListeners = uniqueListeners,
    tags = tags?.deserializeArtistTags(),
)

private fun String.deserializeArtistTags(): List<String> =
    split(",").map(String::trim).filter(String::isNotEmpty)
