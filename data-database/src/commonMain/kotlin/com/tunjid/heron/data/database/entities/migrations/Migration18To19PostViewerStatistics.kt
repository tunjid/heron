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

package com.tunjid.heron.data.database.entities.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration18To19PostViewerStatistics : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        // Migrate postViewerStatistics
        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `postViewerStatistics_new` (
                `postId` TEXT NOT NULL,
                `viewingProfileId` TEXT NOT NULL,
                `likeUri` TEXT DEFAULT NULL,
                `repostUri` TEXT DEFAULT NULL,
                `threadMuted` INTEGER NOT NULL,
                `replyDisabled` INTEGER NOT NULL,
                `embeddingDisabled` INTEGER NOT NULL,
                `pinned` INTEGER NOT NULL,
                PRIMARY KEY(`postId`, `viewingProfileId`),
                FOREIGN KEY(`postId`)
                    REFERENCES `posts`(`cid`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE 
                FOREIGN KEY(`viewingProfileId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        // Remove the old table
        connection.execSQL("DROP TABLE postViewerStatistics")

        // Change the table name to the correct one
        connection.execSQL("ALTER TABLE postViewerStatistics_new RENAME TO postViewerStatistics")
    }
}