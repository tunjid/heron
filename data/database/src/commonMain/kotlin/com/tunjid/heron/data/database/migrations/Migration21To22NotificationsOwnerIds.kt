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

internal object Migration21To22NotificationsOwnerIds : Migration(21, 22) {

    // Destructively migrates notifications to persist the owner id.
    override fun migrate(connection: SQLiteConnection) {
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
                PRIMARY KEY(`uri`),
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
    }
}
