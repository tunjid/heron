package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Upsert
    suspend fun upsertProfiles(
        entities: List<ProfileEntity>,
    )

    @Query(
        """
            SELECT * FROM profiles
            WHERE did IN (:ids)
        """
    )
    fun profiles(
        ids: List<Id>,
    ): Flow<List<ProfileEntity>>
}