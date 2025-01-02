package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PopulatedNotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface NotificationsDao {
    @Query(
        """
            SELECT * FROM notifications
            WHERE indexedAt < :before
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun notifications(
        before: Instant,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedNotificationEntity>>

    @Upsert
    suspend fun upsertNotifications(
        entities: List<NotificationEntity>,
    )
}