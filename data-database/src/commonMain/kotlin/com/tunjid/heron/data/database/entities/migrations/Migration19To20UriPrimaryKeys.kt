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
                SELECT DISTINCT
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

        connection.execSQL("CREATE INDEX `index_posts_uri` ON posts (`uri`);")
        connection.execSQL("CREATE INDEX `index_posts_cid` ON posts (`cid`);")
        connection.execSQL("CREATE INDEX `index_posts_authorId` ON posts (`authorId`);")

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
            SELECT DISTINCT
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

        connection.execSQL("CREATE INDEX `index_lists_uri` ON lists (`uri`);")
        connection.execSQL("CREATE INDEX `index_lists_cid` ON lists (`cid`);")
        connection.execSQL("CREATE INDEX `index_lists_creatorId` ON lists (`creatorId`);")
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
            SELECT DISTINCT
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

        connection.execSQL("CREATE INDEX `index_feedGenerators_uri` ON feedGenerators (`uri`);")
        connection.execSQL("CREATE INDEX `index_feedGenerators_cid` ON feedGenerators (`cid`);")
        connection.execSQL("CREATE INDEX `index_feedGenerators_creatorId` ON feedGenerators (`creatorId`);")
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
            SELECT DISTINCT
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

        connection.execSQL("CREATE INDEX `index_starterPacks_uri` ON starterPacks (`uri`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_cid` ON starterPacks (`cid`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_creatorId` ON starterPacks (`creatorId`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_indexedAt` ON starterPacks (`indexedAt`);")
        connection.execSQL("CREATE INDEX `index_starterPacks_createdAt` ON starterPacks (`createdAt`);")

        // Migrate notifications
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifications_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `reasonSubject` TEXT,
                `associatedPostUri` TEXT,
                `isRead` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`associatedPostUri`) 
                    REFERENCES `posts`(`uri`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
                FOREIGN KEY(`authorId`) 
                    REFERENCES `profiles`(`did`) 
                    ON UPDATE NO ACTION 
                    ON DELETE CASCADE
            )
        """.trimIndent()
        )
        connection.execSQL("DROP TABLE notifications")
        connection.execSQL("ALTER TABLE notifications_new RENAME TO notifications")

        connection.execSQL("CREATE INDEX `index_notifications_uri` ON notifications (`uri`);")
        connection.execSQL("CREATE INDEX `index_notifications_cid` ON notifications (`cid`);")
        connection.execSQL("CREATE INDEX `index_notifications_authorId` ON notifications (`authorId`);")
        connection.execSQL("CREATE INDEX `index_notifications_indexedAt` ON notifications (`indexedAt`);")


        // Migrate join tables


        // Migrate postLikes
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postLikes_new` (
                 `postUri` TEXT NOT NULL,
                 `authorId` TEXT NOT NULL,
                 `createdAt` INTEGER NOT NULL,
                 `indexedAt` INTEGER NOT NULL,
                 PRIMARY KEY(`postUri`, `authorId`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`authorId`)
                     REFERENCES `profiles`(`did`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postLikes")
        connection.execSQL("ALTER TABLE postLikes_new RENAME TO postLikes")

        connection.execSQL("CREATE INDEX `index_postLikes_postUri` ON postLikes (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postLikes_authorId` ON postLikes (`authorId`);")
        connection.execSQL("CREATE INDEX `index_postLikes_createdAt` ON postLikes (`createdAt`);")
        connection.execSQL("CREATE INDEX `index_postLikes_indexedAt` ON postLikes (`indexedAt`);")

        // Migrate postReposts
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postReposts_new` (
                 `postUri` TEXT NOT NULL,
                 `authorId` TEXT NOT NULL,
                 `createdAt` INTEGER NOT NULL,
                 `indexedAt` INTEGER NOT NULL,
                 PRIMARY KEY(`postUri`, `authorId`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`authorId`)
                     REFERENCES `profiles`(`did`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postReposts")
        connection.execSQL("ALTER TABLE postReposts_new RENAME TO postReposts")

        connection.execSQL("CREATE INDEX `index_postReposts_postUri` ON postReposts (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postReposts_authorId` ON postReposts (`authorId`);")
        connection.execSQL("CREATE INDEX `index_postReposts_indexedAt` ON postReposts (`indexedAt`);")

        // Migrate postThreads
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postThreads_new` (
                 `parentPostUri` TEXT NOT NULL,
                 `postUri` TEXT NOT NULL,
                 PRIMARY KEY(`parentPostUri`, `postUri`),
                 FOREIGN KEY(`parentPostUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postThreads")
        connection.execSQL("ALTER TABLE postThreads_new RENAME TO postThreads")

        connection.execSQL("CREATE INDEX `index_postThreads_parentPostUri` ON postThreads (`parentPostUri`);")
        connection.execSQL("CREATE INDEX `index_postThreads_postUri` ON postThreads (`postUri`);")


        // Migrate timelineItems
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `timelineItems_new` (
                 `postUri` TEXT NOT NULL,
                 `viewingProfileId` TEXT,
                 `sourceId` TEXT NOT NULL,
                 `rootPostUri` TEXT,
                 `parentPostUri` TEXT,
                 `grandParentPostAuthorId` TEXT,
                 `reposter` TEXT,
                 `hasMedia` INTEGER NOT NULL DEFAULT false,
                 `isPinned` INTEGER NOT NULL,
                 `indexedAt` INTEGER NOT NULL,
                 `id` TEXT NOT NULL,
                 PRIMARY KEY(`id`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`rootPostUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`parentPostUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
                 FOREIGN KEY(`grandParentPostAuthorId`)
                     REFERENCES `profiles`(`did`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE   
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE timelineItems")
        connection.execSQL("ALTER TABLE timelineItems_new RENAME TO timelineItems")

        connection.execSQL("CREATE INDEX `index_timelineItems_postUri` ON timelineItems (`postUri`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_indexedAt` ON timelineItems (`indexedAt`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_viewingProfileId` ON timelineItems (`viewingProfileId`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_sourceId` ON timelineItems (`sourceId`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_rootPostUri` ON timelineItems (`rootPostUri`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_parentPostUri` ON timelineItems (`parentPostUri`);")
        connection.execSQL("CREATE INDEX `index_timelineItems_grandParentPostAuthorId` ON timelineItems (`grandParentPostAuthorId`);")

        // Migrate postEmbeds
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postPosts_new` (
                 `postUri` TEXT NOT NULL,
                 `embeddedPostUri` TEXT NOT NULL,
                 PRIMARY KEY(`postUri`, `embeddedPostUri`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`embeddedPostUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postPosts")
        connection.execSQL("ALTER TABLE postPosts_new RENAME TO postPosts")

        connection.execSQL("CREATE INDEX `index_postPosts_postUri` ON postPosts (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postPosts_embeddedPostUri` ON postPosts (`embeddedPostUri`);")

        // Migrate postViewerStatistics
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postViewerStatistics_new` (
                 `postUri` TEXT NOT NULL,
                 `viewingProfileId` TEXT NOT NULL,
                 `likeUri` TEXT DEFAULT NULL,
                 `repostUri` TEXT DEFAULT NULL,
                 `threadMuted` INTEGER NOT NULL,
                 `replyDisabled` INTEGER NOT NULL,
                 `embeddingDisabled` INTEGER NOT NULL,
                 `pinned` INTEGER NOT NULL,
                 PRIMARY KEY(`postUri`, `viewingProfileId`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`viewingProfileId`)
                     REFERENCES `profiles`(`did`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postViewerStatistics")
        connection.execSQL("ALTER TABLE postViewerStatistics_new RENAME TO postViewerStatistics")

        connection.execSQL("CREATE INDEX `index_postViewerStatistics_postUri` ON postViewerStatistics (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postViewerStatistics_viewingProfileId` ON postViewerStatistics (`viewingProfileId`);")

        // Migrate postAuthors
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postAuthors_new` (
                 `postUri` TEXT NOT NULL,
                 `authorId` TEXT NOT NULL,
                 PRIMARY KEY(`postUri`, `authorId`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`authorId`)
                     REFERENCES `profiles`(`did`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postAuthors")
        connection.execSQL("ALTER TABLE postAuthors_new RENAME TO postAuthors")

        connection.execSQL("CREATE INDEX `index_postAuthors_postUri` ON postAuthors (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postAuthors_authorId` ON postAuthors (`authorId`);")

        // Migrate postImages
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postImages_new` (
                 `postUri` TEXT NOT NULL,
                 `imageUri` TEXT NOT NULL,
                 PRIMARY KEY(`postUri`, `imageUri`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`imageUri`)
                     REFERENCES `images`(`fullSize`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postImages")
        connection.execSQL("ALTER TABLE postImages_new RENAME TO postImages")

        connection.execSQL("CREATE INDEX `index_postImages_postUri` ON postImages (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postImages_imageUri` ON postImages (`imageUri`);")

        // Migrate postExternalEmbeds
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postExternalEmbeds_new` (
                 `postUri` TEXT NOT NULL,
                 `externalEmbedUri` TEXT NOT NULL,
                 PRIMARY KEY(`postUri`, `externalEmbedUri`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`externalEmbedUri`)
                     REFERENCES `externalEmbeds`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postExternalEmbeds")
        connection.execSQL("ALTER TABLE postExternalEmbeds_new RENAME TO postExternalEmbeds")

        connection.execSQL("CREATE INDEX `index_postExternalEmbeds_postUri` ON postExternalEmbeds (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postExternalEmbeds_externalEmbedUri` ON postExternalEmbeds (`externalEmbedUri`);")

        // Migrate postVideos
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `postVideos_new` (
                 `postUri` TEXT NOT NULL,
                 `videoId` TEXT NOT NULL,
                 PRIMARY KEY(`postUri`, `videoId`),
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`videoId`)
                     REFERENCES `videos`(`cid`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE postVideos")
        connection.execSQL("ALTER TABLE postVideos_new RENAME TO postVideos")

        connection.execSQL("CREATE INDEX `index_postVideos_postUri` ON postVideos (`postUri`);")
        connection.execSQL("CREATE INDEX `index_postVideos_videoId` ON postVideos (`videoId`);")

        // Migrate messagePosts
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `messagePosts_new` (
                 `messageId` TEXT NOT NULL,
                 `postUri` TEXT NOT NULL,
                 PRIMARY KEY(`messageId`, `postUri`),
                 FOREIGN KEY(`messageId`)
                     REFERENCES `messages`(`id`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`postUri`)
                     REFERENCES `posts`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE messagePosts")
        connection.execSQL("ALTER TABLE messagePosts_new RENAME TO messagePosts")

        connection.execSQL("CREATE INDEX `index_messagePosts_messageId` ON messagePosts (`messageId`);")
        connection.execSQL("CREATE INDEX `index_messagePosts_postUri` ON messagePosts (`postUri`);")

        // Migrate messageLists
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `messageLists_new` (
                 `messageId` TEXT NOT NULL,
                 `listUri` TEXT NOT NULL,
                 PRIMARY KEY(`messageId`, `listUri`),
                 FOREIGN KEY(`messageId`)
                     REFERENCES `messages`(`id`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`listUri`)
                     REFERENCES `lists`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE messageLists")
        connection.execSQL("ALTER TABLE messageLists_new RENAME TO messageLists")

        connection.execSQL("CREATE INDEX `index_messageLists_messageId` ON messageLists (`messageId`);")
        connection.execSQL("CREATE INDEX `index_messageLists_listUri` ON messageLists (`listUri`);")

        // Migrate messageStarterPacks
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `messageStarterPacks_new` (
                 `messageId` TEXT NOT NULL,
                 `starterPackUri` TEXT NOT NULL,
                 PRIMARY KEY(`messageId`, `starterPackUri`),
                 FOREIGN KEY(`messageId`)
                     REFERENCES `messages`(`id`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`starterPackUri`)
                     REFERENCES `starterPacks`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE messageStarterPacks")
        connection.execSQL("ALTER TABLE messageStarterPacks_new RENAME TO messageStarterPacks")

        connection.execSQL("CREATE INDEX `index_messageStarterPacks_messageId` ON messageStarterPacks (`messageId`);")
        connection.execSQL("CREATE INDEX `index_messageStarterPacks_starterPackUri` ON messageStarterPacks (`starterPackUri`);")

        // Migrate messageFeedGenerators
        connection.execSQL(
            """
             CREATE TABLE IF NOT EXISTS `messageFeedGenerators_new` (
                 `messageId` TEXT NOT NULL,
                 `feedGeneratorUri` TEXT NOT NULL,
                 PRIMARY KEY(`messageId`, `feedGeneratorUri`),
                 FOREIGN KEY(`messageId`)
                     REFERENCES `messages`(`id`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE,
                 FOREIGN KEY(`feedGeneratorUri`)
                     REFERENCES `feedGenerators`(`uri`)
                     ON UPDATE NO ACTION
                     ON DELETE CASCADE
             )
             """.trimIndent()
        )

        connection.execSQL("DROP TABLE messageFeedGenerators")
        connection.execSQL("ALTER TABLE messageFeedGenerators_new RENAME TO messageFeedGenerators")

        connection.execSQL("CREATE INDEX `index_messageFeedGenerators_messageId` ON messageFeedGenerators (`messageId`);")
        connection.execSQL("CREATE INDEX `index_messageFeedGenerators_feedGeneratorUri` ON messageFeedGenerators (`feedGeneratorUri`);")
    }
}