package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri

@Entity(
    tableName = "rockSkyArtists",
    indices = [
        Index(value = ["uri"]),
    ],
)
data class RockSkyArtistEntity(
    @PrimaryKey
    val uri: ArtistUri,
    val cid: ArtistId,
    val name: String,
    val picture: ImageUri?,
    val playCount: Long?,
    val uniqueListeners: Long?,
    val tags: String?,
)

fun RockSkyArtistEntity.asExternalModel() = RockSkyArtist(
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
