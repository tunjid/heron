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

internal object Migration37To38StandardSubscriptionViewerIds : Migration(37, 38) {

    // Rebuilds standardSubscriptions with a composite primary key of (uri, viewingProfileId),
    // and adds a sortedAt column to standardPublications.
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
                ALTER TABLE standardPublications
                ADD COLUMN `sortedAt` INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_standardPublications_sortedAt` ON `standardPublications` (`sortedAt`)")

        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `standardSubscriptions_new` (
                    `uri` TEXT NOT NULL,
                    `cid` TEXT,
                    `publicationUri` TEXT NOT NULL,
                    `viewingProfileId` TEXT NOT NULL,
                    `sortedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`publicationUri`, `viewingProfileId`),
                    FOREIGN KEY(`publicationUri`)
                        REFERENCES `standardPublications`(`uri`)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
                    FOREIGN KEY(`viewingProfileId`)
                        REFERENCES `profiles`(`did`)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                    )

            """.trimIndent(),
        )

        connection.execSQL(
            """
                INSERT OR IGNORE INTO standardSubscriptions_new (
                    uri,
                    cid,
                    publicationUri,
                    viewingProfileId,
                    sortedAt
                )
                SELECT
                    uri,
                    NULL,
                    publicationUri,
                    viewingProfileId,
                    0
                FROM standardSubscriptions
            """.trimIndent(),
        )

        connection.execSQL("DROP TABLE standardSubscriptions")
        connection.execSQL("ALTER TABLE standardSubscriptions_new RENAME TO standardSubscriptions")

        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_standardSubscriptions_uri` ON `standardSubscriptions` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_standardSubscriptions_publicationUri` ON `standardSubscriptions` (`publicationUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_standardSubscriptions_viewingProfileId` ON `standardSubscriptions` (`viewingProfileId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_standardSubscriptions_sortedAt` ON `standardSubscriptions` (`sortedAt`)")
    }
}
