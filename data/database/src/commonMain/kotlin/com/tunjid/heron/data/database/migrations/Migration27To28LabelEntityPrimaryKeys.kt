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

internal object Migration27To28LabelEntityPrimaryKeys : Migration(27, 28) {

    override fun migrate(connection: SQLiteConnection) {
        // Migrate labels
        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `labels_new` (
                `cid` TEXT,
                `uri` TEXT NOT NULL,
                `creatorId` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                `version` INTEGER,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`, `value`),
                FOREIGN KEY(`creatorId`)
                    REFERENCES `profiles`(`did`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
                )
            """.trimIndent(),
        )

        connection.execSQL(
            """
                INSERT INTO labels_new (
                    cid,
                    uri,
                    creatorId,
                    value,
                    version,
                    createdAt
                )
                SELECT
                    cid,
                    uri,
                    creatorId,
                    value,
                    version,
                    createdAt
                FROM labels
            """.trimIndent(),
        )

        connection.execSQL("DROP TABLE labels")
        connection.execSQL("ALTER TABLE labels_new RENAME TO labels")

        connection.execSQL("CREATE INDEX `index_labels_createdAt` ON labels (`createdAt`);")
        connection.execSQL("CREATE INDEX `index_labels_uri` ON labels (`uri`);")
        connection.execSQL("CREATE INDEX `index_labels_creatorId` ON labels (`creatorId`);")
        connection.execSQL("CREATE INDEX `index_labels_value` ON labels (`value`);")
    }
}
