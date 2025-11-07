package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.LabelDefinitionEntity

@Dao
interface LabelDefinitionDao {

    @Upsert
    suspend fun upsertLabelValueDefinitions(
        entities: List<LabelDefinitionEntity>,
    )
}
