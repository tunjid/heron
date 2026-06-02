/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PopulatedNotificationEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationsDao {
    @Query(
        """
            SELECT * FROM notifications
            LEFT JOIN profileViewerStates
            ON notifications.authorId = profileViewerStates.otherProfileId
            AND ownerId = profileViewerStates.profileId
            WHERE ownerId = :ownerId
            AND indexedAt < :before
            ORDER BY indexedAt DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun notifications(
        ownerId: String,
        before: Instant,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedNotificationEntity>>

    @Query(
        """
            SELECT * FROM notifications
            LEFT JOIN profileViewerStates
            ON notifications.authorId = profileViewerStates.otherProfileId
            AND ownerId = profileViewerStates.profileId
            WHERE ownerId = :ownerId
            AND isRead = 0
            AND indexedAt >= :after
            ORDER BY indexedAt DESC
        """
    )
    fun unreadNotifications(
        ownerId: String,
        after: Instant,
    ): Flow<List<PopulatedNotificationEntity>>

    @Upsert suspend fun upsertNotifications(entities: List<NotificationEntity>)

    @Query(
        """
        DELETE FROM notifications
    """
    )
    suspend fun deleteAllNotifications()
}
