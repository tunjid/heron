package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.VideoEntity

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