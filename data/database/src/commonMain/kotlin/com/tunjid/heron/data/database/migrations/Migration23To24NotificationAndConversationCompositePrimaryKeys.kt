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

internal object Migration23To24NotificationAndConversationCompositePrimaryKeys : Migration(23, 24) {

    override fun migrate(connection: SQLiteConnection) {
        // Migrate notifications
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifications_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `reasonSubject` TEXT,
                `associatedPostUri` TEXT,
                `isRead` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`, `ownerId`),
                FOREIGN KEY(`associatedPostUri`)
                    REFERENCES `posts`(`uri`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE,
                FOREIGN KEY(`authorId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE,
                FOREIGN KEY(`ownerId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO notifications_new (
                cid,
                uri,
                authorId,
                ownerId,
                reason,
                reasonSubject,
                associatedPostUri,
                isRead,
                indexedAt
            )
            SELECT
                cid,
                uri,
                authorId,
                ownerId,
                reason,
                reasonSubject,
                associatedPostUri,
                isRead,
                indexedAt
            FROM notifications
            """
                .trimIndent()
        )

        connection.execSQL("DROP TABLE notifications")
        connection.execSQL("ALTER TABLE notifications_new RENAME TO notifications")

        connection.execSQL("CREATE INDEX `index_notifications_uri` ON notifications (`uri`);")
        connection.execSQL("CREATE INDEX `index_notifications_cid` ON notifications (`cid`);")
        connection.execSQL(
            "CREATE INDEX `index_notifications_authorId` ON notifications (`authorId`);"
        )
        connection.execSQL(
            "CREATE INDEX `index_notifications_ownerId` ON notifications (`ownerId`);"
        )
        connection.execSQL(
            "CREATE INDEX `index_notifications_indexedAt` ON notifications (`indexedAt`);"
        )

        // Destructively migrate conversation members, the conversation they are a part of cannot
        // be easily obtained
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversationMembers_new` (
                `conversationId` TEXT NOT NULL,
                `conversationOwnerId` TEXT NOT NULL,
                `memberId` TEXT NOT NULL,
                PRIMARY KEY(`conversationId`, `memberId`),
                FOREIGN KEY(`conversationId`, `conversationOwnerId`)
                    REFERENCES `conversations`(`id`, `ownerId`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                FOREIGN KEY(`memberId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        connection.execSQL("DROP TABLE conversationMembers")
        connection.execSQL("ALTER TABLE conversationMembers_new RENAME TO conversationMembers")

        // Destructively migrate messages, the conversation they are a part of cannot
        // be easily obtained
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messages_new` (
                `id` TEXT NOT NULL,
                `rev` TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `senderId` TEXT NOT NULL,
                `conversationId` TEXT NOT NULL,
                `conversationOwnerId` TEXT NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `sentAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`conversationId`, `conversationOwnerId`)
                    REFERENCES `conversations`(`id`, `ownerId`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                FOREIGN KEY(`senderId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        connection.execSQL("DROP TABLE messages")
        connection.execSQL("ALTER TABLE messages_new RENAME TO messages")

        connection.execSQL("CREATE INDEX `index_messages_sentAt` ON messages (`sentAt`);")

        // Migrate conversations
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversations_new` (
                `id` TEXT NOT NULL,
                `rev` TEXT NOT NULL,
                `ownerId` TEXT NOT NULL,
                `lastMessageId` TEXT,
                `lastReactedToMessageId` TEXT,
                `muted` INTEGER NOT NULL,
                `status` TEXT,
                `unreadCount` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `ownerId`),
                FOREIGN KEY(`ownerId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO conversations_new (
                id,
                rev,
                ownerId,
                lastMessageId,
                lastReactedToMessageId,
                muted,
                status,
                unreadCount
            )
            SELECT
                id,
                rev,
                ownerId,
                lastMessageId,
                lastReactedToMessageId,
                muted,
                status,
                unreadCount
            FROM conversations
            """
                .trimIndent()
        )

        connection.execSQL("DROP TABLE conversations")
        connection.execSQL("ALTER TABLE conversations_new RENAME TO conversations")

        connection.execSQL("CREATE INDEX `index_conversations_id` ON conversations (`id`);")
        connection.execSQL(
            "CREATE INDEX `index_conversations_ownerId` ON conversations (`ownerId`);"
        )
    }
}
