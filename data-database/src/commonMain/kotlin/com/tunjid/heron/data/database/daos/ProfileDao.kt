package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.partial
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import com.tunjid.heron.data.database.entities.profile.partial
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query(
        """
            SELECT * FROM profiles
            WHERE did IN (:ids)
            OR handle IN (:ids)
        """
    )
    fun profiles(
        ids: List<Id>,
    ): Flow<List<ProfileEntity>>

    @Query(
        """
            SELECT * FROM profileProfileRelationships
            INNER JOIN profiles
            ON profileId = did
            WHERE profileId = :profileId
                AND otherProfileId IN (:otherProfileIds)
            OR handle = :profileId
                AND otherProfileId IN (:otherProfileIds)
        """
    )
    fun relationships(
        profileId: String,
        otherProfileIds: Set<Id>,
    ): Flow<List<ProfileProfileRelationshipsEntity>>

    @Upsert
    suspend fun upsertProfiles(
        entities: List<ProfileEntity>,
    )

    @Transaction
    suspend fun insertOrPartiallyUpdateProfiles(
        entities: List<ProfileEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = ProfileEntity::partial,
        insertEntities = ::insertOrIgnoreProfiles,
        updatePartials = ::updatePartialProfiles
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreProfiles(
        entities: List<ProfileEntity>,
    ): List<Long>

    @Update(entity = ProfileEntity::class)
    suspend fun updatePartialProfiles(
        entities: List<ProfileEntity.Partial>,
    )

    @Upsert
    suspend fun upsertProfileProfileRelationships(
        entities: List<ProfileProfileRelationshipsEntity>,
    )

    @Transaction
    suspend fun insertOrPartiallyUpdateProfileProfileRelationships(
        entities: List<ProfileProfileRelationshipsEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = ProfileProfileRelationshipsEntity::partial,
        insertEntities = ::insertOrIgnoreProfileProfileRelationships,
        updatePartials = ::updatePartialProfileProfileRelationships
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreProfileProfileRelationships(
        entities: List<ProfileProfileRelationshipsEntity>,
    ): List<Long>

    @Update(entity = ProfileProfileRelationshipsEntity::class)
    suspend fun updatePartialProfileProfileRelationships(
        entities: List<ProfileProfileRelationshipsEntity.Partial>,
    )

}