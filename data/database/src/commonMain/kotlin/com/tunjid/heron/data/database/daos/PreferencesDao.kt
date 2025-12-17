package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.preferences.MutedWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {

    @Query(
        """
            SELECT * FROM muted_words
            WHERE viewingProfileId = :viewingProfileId""",
    )
    fun mutedWords(
        viewingProfileId: ProfileId,
    ): Flow<List<MutedWordEntity>>

    @Upsert
    suspend fun upsertMutedWords(
        entities: List<MutedWordEntity>,
    )
}
