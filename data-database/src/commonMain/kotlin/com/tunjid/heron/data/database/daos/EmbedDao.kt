package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity

@Dao
interface EmbedDao {

    @Upsert
    suspend fun upsertExternalEmbeds(
        entities: List<ExternalEmbedEntity>,
    )

    @Upsert
    suspend fun upsertImages(
        entities: List<ImageEntity>,
    )

    @Upsert
    suspend fun upsertVideos(
        entities: List<VideoEntity>,
    )
}