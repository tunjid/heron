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

internal object Migration17To18TimelineViewer : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        // Migrate timelineItems

        // Add columns
        connection.execSQL("ALTER TABLE timelineItems ADD COLUMN viewingProfileId TEXT DEFAULT NULL;")

        // Create indices
        connection.execSQL("CREATE INDEX `index_timelineItems_viewingProfileId` ON timelineItems (`viewingProfileId`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_sourceId` ON timelineItems (`sourceId`);")

        // Migrate timelinePreferences
        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `timelinePreferences_new` (
                `sourceId` TEXT NOT NULL,
                `viewingProfileId` TEXT,
                `lastFetchedAt` INTEGER NOT NULL,
                `preferredPresentation` TEXT,
                `id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        // Remove the old table
        connection.execSQL("DROP TABLE timelinePreferences")

        // Change the table name to the correct one
        connection.execSQL("ALTER TABLE timelinePreferences_new RENAME TO timelinePreferences")

        // Create indices
        connection.execSQL("CREATE INDEX `index_timelinePreferences_viewingProfileId` ON timelinePreferences (`viewingProfileId`);")
        connection.execSQL("CREATE INDEX `index_timelinePreferences_sourceId` ON timelinePreferences (`sourceId`);")
    }
}
