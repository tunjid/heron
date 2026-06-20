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

package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds group conversation support:
 * - `conversations` gains nullable group metadata columns (`kind`, `name`,
 *   `memberCount`, `lockStatus`).
 * - `messages.senderId` becomes nullable (system messages have no sender) and a
 *   `base64EncodedSystemContent` column is added. SQLite cannot alter a column's
 *   nullability in place, so the table is recreated.
 */
internal object Migration45To46GroupConversations : Migration(45, 46) {

    override fun migrate(connection: SQLiteConnection) {
        // Group metadata on conversations — additive, nullable columns.
        connection.execSQL("ALTER TABLE conversations ADD COLUMN `kind` TEXT")
        connection.execSQL("ALTER TABLE conversations ADD COLUMN `name` TEXT")
        connection.execSQL("ALTER TABLE conversations ADD COLUMN `memberCount` INTEGER")
        connection.execSQL("ALTER TABLE conversations ADD COLUMN `lockStatus` TEXT")

        // Recreate messages with a nullable senderId and the system content column.
        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `messages_new` (
                    `id` TEXT NOT NULL,
                    `rev` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `senderId` TEXT,
                    `conversationId` TEXT NOT NULL,
                    `conversationOwnerId` TEXT NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    `sentAt` INTEGER NOT NULL,
                    `base64EncodedMetadata` TEXT DEFAULT NULL,
                    `base64EncodedSystemContent` TEXT DEFAULT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`conversationId`, `conversationOwnerId`)
                        REFERENCES `conversations`(`id`, `ownerId`)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
                    FOREIGN KEY(`senderId`)
                        REFERENCES `profiles`(`did`)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
            """.trimIndent(),
        )

        connection.execSQL(
            """
                INSERT INTO messages_new (
                    id,
                    rev,
                    text,
                    senderId,
                    conversationId,
                    conversationOwnerId,
                    isDeleted,
                    sentAt,
                    base64EncodedMetadata
                )
                SELECT
                    id,
                    rev,
                    text,
                    senderId,
                    conversationId,
                    conversationOwnerId,
                    isDeleted,
                    sentAt,
                    base64EncodedMetadata
                FROM messages
            """.trimIndent(),
        )

        connection.execSQL("DROP TABLE messages")
        connection.execSQL("ALTER TABLE messages_new RENAME TO messages")

        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_sentAt` ON messages (`sentAt`)")
    }
}
