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

internal object Migration22To23ConversationOwnerIds : Migration(22, 23) {

    // Destructively migrates conversations to persist the owner id.
    override fun migrate(connection: SQLiteConnection) {
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
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`ownerId`)
                        REFERENCES `profiles`(`did`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
            """.trimIndent(),
        )

        connection.execSQL("DROP TABLE conversations")
        connection.execSQL("ALTER TABLE conversations_new RENAME TO conversations")

        connection.execSQL("CREATE INDEX `index_conversations_id` ON conversations (`id`);")
        connection.execSQL("CREATE INDEX `index_conversations_ownerId` ON conversations (`ownerId`);")
    }
}
