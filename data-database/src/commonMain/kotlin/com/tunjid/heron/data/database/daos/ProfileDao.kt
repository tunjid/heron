package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.ProfileEntity

@Dao
interface ProfileDao {

    @Upsert
    suspend fun upsertProfiles(
        entities: List<ProfileEntity>,
    )
}