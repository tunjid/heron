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

internal object Migration12To13FeedAndListsCreatedAt : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        // Add columns
        connection.execSQL(
            "ALTER TABLE feedGenerators ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0;"
        )
        connection.execSQL("ALTER TABLE lists ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0;")

        // Create indices
        connection.execSQL(
            "CREATE INDEX `index_feedGenerators_createdAt` ON feedGenerators (`createdAt`);"
        )
        connection.execSQL("CREATE INDEX `index_lists_createdAt` ON lists (`createdAt`);")
        connection.execSQL(
            "CREATE INDEX `index_starterPacks_createdAt` ON starterPacks (`createdAt`);"
        )
    }
}
