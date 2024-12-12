package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.partial
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Transaction
    suspend fun upsertProfiles(
        entities: List<ProfileEntity>
    ) = upsert(
        items = entities,
        entityMapper = ProfileEntity::partial,
        insertMany = ::insertOrIgnoreProfiles,
        updateMany = ::updatePartialProfiles
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreProfiles(
        entities: List<ProfileEntity>,
    ): List<Long>

    @Update(entity = ProfileEntity::class)
    suspend fun updatePartialProfiles(
        entities: List<ProfileEntity.Partial>,
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