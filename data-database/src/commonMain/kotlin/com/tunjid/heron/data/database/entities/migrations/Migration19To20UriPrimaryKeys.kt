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

internal object Migration19To20UriPrimaryKeys : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        // Migrate posts
        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `posts_new` (
                    `cid` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `authorId` TEXT NOT NULL,
                    `replyCount` INTEGER,
                    `repostCount` INTEGER,
                    `likeCount` INTEGER,
                    `quoteCount` INTEGER,
                    `indexedAt` INTEGER NOT NULL,
                    `text` TEXT,
                    `base64EncodedRecord` TEXT,
                    `createdAt` INTEGER,
                    PRIMARY KEY(`uri`),
                    FOREIGN KEY(`authorId`) 
                        REFERENCES `profiles`(`did`) 
                        ON UPDATE NO ACTION 
                        ON DELETE CASCADE
                )
            """.trimIndent()
        )
        connection.execSQL(
            """
                INSERT INTO posts_new (
                    cid, 
                    uri, 
                    authorId, 
                    replyCount,
                    repostCount, 
                    likeCount, 
                    quoteCount, 
                    indexedAt, 
                    text, 
                    base64EncodedRecord, 
                    createdAt
                )
                SELECT 
                    cid, 
                    uri, 
                    authorId, 
                    replyCount, 
                    repostCount, 
                    likeCount, 
                    quoteCount, 
                    indexedAt, 
                    text, 
                    base64EncodedRecord, 
                    createdAt 
                FROM posts
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE posts")
        connection.execSQL("ALTER TABLE posts_new RENAME TO posts")

        connection.execSQL("CREATE INDEX `index_posts_cid` ON posts (`cid`);")

        // Migrate lists
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lists_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `creatorId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `avatar` TEXT,
                `listItemCount` INTEGER,
                `purpose` TEXT NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`creatorId`) 
                    REFERENCES `profiles`(`did`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
            )
        """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO lists_new (
                cid,
                uri,
                creatorId,
                name,
                description,
                avatar,
                listItemCount,
                purpose,
                indexedAt,
                createdAt
            )
            SELECT
                cid,
                uri,
                creatorId,
                name,
                description,
                avatar,
                listItemCount,
                purpose,
                indexedAt,
                createdAt
            FROM lists
        """.trimIndent()
        )
        connection.execSQL("DROP TABLE lists")
        connection.execSQL("ALTER TABLE lists_new RENAME TO lists")

        connection.execSQL("CREATE INDEX `index_lists_cid` ON lists (`cid`);")
        connection.execSQL("CREATE INDEX `index_lists_indexedAt` ON lists (`indexedAt`);")
        connection.execSQL("CREATE INDEX `index_lists_createdAt` ON lists (`createdAt`);")

        // Migrate feed_generators
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `feedGenerators_new` (
                `cid` TEXT NOT NULL,
                `did` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `avatar` TEXT,
                `likeCount` INTEGER,
                `creatorId` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `description` TEXT,
                `acceptsInteractions` INTEGER,
                `contentMode` TEXT,
                `indexedAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`creatorId`) 
                    REFERENCES `profiles`(`did`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
            )
        """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO feedGenerators_new (
                cid,
                did,
                uri,
                avatar,
                likeCount,
                creatorId,
                displayName,
                description,
                acceptsInteractions,
                contentMode,
                indexedAt,
                createdAt
            )
            SELECT
                cid,
                did,
                uri,
                avatar,
                likeCount,
                creatorId,
                displayName,
                description,
                acceptsInteractions,
                contentMode,
                indexedAt,
                createdAt
            FROM feedGenerators
        """.trimIndent()
        )
        connection.execSQL("DROP TABLE feedGenerators")
        connection.execSQL("ALTER TABLE feedGenerators_new RENAME TO feedGenerators")

        connection.execSQL("CREATE INDEX `index_feedGenerators_cid` ON feedGenerators (`cid`);")
        connection.execSQL("CREATE INDEX `index_feedGenerators_indexedAt` ON feedGenerators (`indexedAt`);")
        connection.execSQL("CREATE INDEX `index_feedGenerators_createdAt` ON feedGenerators (`createdAt`);")

        // Migrate starter_packs
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `starterPacks_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `creatorId` TEXT NOT NULL,
                `listUri` TEXT,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `joinedWeekCount` INTEGER,
                `joinedAllTimeCount` INTEGER,
                `indexedAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`creatorId`) 
                    REFERENCES `profiles`(`did`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
            )
        """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO starterPacks_new (
                cid,
                uri,
                creatorId,
                listUri,
                name,
                description,
                joinedWeekCount,
                joinedAllTimeCount,
                indexedAt,
                createdAt
            )
            SELECT
                cid,
                uri,
                creatorId,
                listUri,
                name,
                description,
                joinedWeekCount,
                joinedAllTimeCount,
                indexedAt,
                createdAt
            FROM starterPacks
        """.trimIndent()
        )
        connection.execSQL("DROP TABLE starterPacks")
        connection.execSQL("ALTER TABLE starterPacks_new RENAME TO starterPacks")

        connection.execSQL("CREATE INDEX `index_starterPacks_cid` ON starterPacks (`cid`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_indexedAt` ON starterPacks (`indexedAt`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_createdAt` ON starterPacks (`createdAt`);")

        // Migrate notifications
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifications_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `authorDid` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `reasonSubject` TEXT,
                `associatedPostId` TEXT,
                `isRead` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`associatedPostUri`) 
                    REFERENCES `posts`(`uri`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
                FOREIGN KEY(`authorDid`) 
                    REFERENCES `profiles`(`did`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
            )
        """.trimIndent()
        )
        connection.execSQL("DROP TABLE notifications")
        connection.execSQL("ALTER TABLE notifications_new RENAME TO notifications")

        connection.execSQL("CREATE INDEX `index_notifications_cid` ON notifications (`cid`);")
        connection.execSQL("CREATE INDEX `index_notifications_indexedAt` ON notifications (`indexedAt`);")
    }
}