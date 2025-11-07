package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.LabelEntity
import com.tunjid.heron.data.database.entities.LabelerEntity

@Dao
interface LabelerDao {
    @Upsert
    suspend fun upsertLabelers(
        entities: List<LabelerEntity>,
    )
}
