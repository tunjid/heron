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

internal object Migration33To34OnUpdateForeignKey : Migration(33, 34) {
    override fun migrate(connection: SQLiteConnection) {
        // postExternalEmbeds
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postExternalEmbeds_new` (
                `postUri` TEXT NOT NULL,
                `externalEmbedUri` TEXT NOT NULL,
                PRIMARY KEY(`postUri`, `externalEmbedUri`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`externalEmbedUri`) REFERENCES `externalEmbeds`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postExternalEmbeds_new SELECT * FROM postExternalEmbeds")
        connection.execSQL("DROP TABLE postExternalEmbeds")
        connection.execSQL("ALTER TABLE postExternalEmbeds_new RENAME TO postExternalEmbeds")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postExternalEmbeds_postUri` ON `postExternalEmbeds` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postExternalEmbeds_externalEmbedUri` ON `postExternalEmbeds` (`externalEmbedUri`)")

        // postImages
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postImages_new` (
                `postUri` TEXT NOT NULL,
                `imageUri` TEXT NOT NULL,
                PRIMARY KEY(`postUri`, `imageUri`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`imageUri`) REFERENCES `images`(`fullSize`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postImages_new SELECT * FROM postImages")
        connection.execSQL("DROP TABLE postImages")
        connection.execSQL("ALTER TABLE postImages_new RENAME TO postImages")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postImages_postUri` ON `postImages` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postImages_imageUri` ON `postImages` (`imageUri`)")

        // postVideos
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postVideos_new` (
                `postUri` TEXT NOT NULL,
                `videoId` TEXT NOT NULL,
                PRIMARY KEY(`postUri`, `videoId`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`videoId`) REFERENCES `videos`(`cid`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postVideos_new SELECT * FROM postVideos")
        connection.execSQL("DROP TABLE postVideos")
        connection.execSQL("ALTER TABLE postVideos_new RENAME TO postVideos")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postVideos_postUri` ON `postVideos` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postVideos_videoId` ON `postVideos` (`videoId`)")

        // postPosts
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postPosts_new` (
                `postUri` TEXT NOT NULL,
                `embeddedPostUri` TEXT NOT NULL,
                PRIMARY KEY(`postUri`, `embeddedPostUri`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`embeddedPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postPosts_new SELECT * FROM postPosts")
        connection.execSQL("DROP TABLE postPosts")
        connection.execSQL("ALTER TABLE postPosts_new RENAME TO postPosts")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postPosts_postUri` ON `postPosts` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postPosts_embeddedPostUri` ON `postPosts` (`embeddedPostUri`)")

        // posts
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
                `hasThreadGate` INTEGER,
                `indexedAt` INTEGER NOT NULL,
                `text` TEXT,
                `base64EncodedRecord` TEXT,
                `embeddedRecordUri` TEXT,
                `createdAt` INTEGER,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`authorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO posts_new SELECT * FROM posts")
        connection.execSQL("DROP TABLE posts")
        connection.execSQL("ALTER TABLE posts_new RENAME TO posts")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_uri` ON `posts` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_cid` ON `posts` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_authorId` ON `posts` (`authorId`)")

        // postAuthors
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postAuthors_new` (
                `postUri` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                PRIMARY KEY(`postUri`, `authorId`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`authorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postAuthors_new SELECT * FROM postAuthors")
        connection.execSQL("DROP TABLE postAuthors")
        connection.execSQL("ALTER TABLE postAuthors_new RENAME TO postAuthors")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postAuthors_postUri` ON `postAuthors` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postAuthors_authorId` ON `postAuthors` (`authorId`)")

        // postThreads
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postThreads_new` (
                `parentPostUri` TEXT NOT NULL,
                `postUri` TEXT NOT NULL,
                PRIMARY KEY(`parentPostUri`, `postUri`),
                FOREIGN KEY(`parentPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postThreads_new SELECT * FROM postThreads")
        connection.execSQL("DROP TABLE postThreads")
        connection.execSQL("ALTER TABLE postThreads_new RENAME TO postThreads")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postThreads_parentPostUri` ON `postThreads` (`parentPostUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postThreads_postUri` ON `postThreads` (`postUri`)")

        // postViewerStatistics
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
                `bookmarked` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`postUri`, `viewingProfileId`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`viewingProfileId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postViewerStatistics_new SELECT * FROM postViewerStatistics")
        connection.execSQL("DROP TABLE postViewerStatistics")
        connection.execSQL("ALTER TABLE postViewerStatistics_new RENAME TO postViewerStatistics")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postViewerStatistics_postUri` ON `postViewerStatistics` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postViewerStatistics_viewingProfileId` ON `postViewerStatistics` (`viewingProfileId`)")

        // profileViewerStates
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `profileViewerStates_new` (
                `profileId` TEXT NOT NULL,
                `otherProfileId` TEXT NOT NULL,
                `muted` INTEGER,
                `mutedByList` TEXT,
                `blockedBy` INTEGER,
                `blocking` TEXT,
                `blockingByList` TEXT,
                `following` TEXT,
                `followedBy` TEXT,
                `commonFollowersCount` INTEGER,
                PRIMARY KEY(`profileId`, `otherProfileId`),
                FOREIGN KEY(`profileId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`otherProfileId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO profileViewerStates_new SELECT * FROM profileViewerStates")
        connection.execSQL("DROP TABLE profileViewerStates")
        connection.execSQL("ALTER TABLE profileViewerStates_new RENAME TO profileViewerStates")

        // postLikes
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postLikes_new` (
                `postUri` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                PRIMARY KEY(`postUri`, `authorId`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`authorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postLikes_new SELECT * FROM postLikes")
        connection.execSQL("DROP TABLE postLikes")
        connection.execSQL("ALTER TABLE postLikes_new RENAME TO postLikes")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postLikes_postUri` ON `postLikes` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postLikes_authorId` ON `postLikes` (`authorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postLikes_createdAt` ON `postLikes` (`createdAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postLikes_indexedAt` ON `postLikes` (`indexedAt`)")

        // postReposts
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `postReposts_new` (
                `postUri` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                PRIMARY KEY(`postUri`, `authorId`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`authorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO postReposts_new SELECT * FROM postReposts")
        connection.execSQL("DROP TABLE postReposts")
        connection.execSQL("ALTER TABLE postReposts_new RENAME TO postReposts")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postReposts_postUri` ON `postReposts` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postReposts_authorId` ON `postReposts` (`authorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_postReposts_indexedAt` ON `postReposts` (`indexedAt`)")

        // labels
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
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO labels_new SELECT * FROM labels")
        connection.execSQL("DROP TABLE labels")
        connection.execSQL("ALTER TABLE labels_new RENAME TO labels")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_createdAt` ON `labels` (`createdAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_uri` ON `labels` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_creatorId` ON `labels` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_value` ON `labels` (`value`)")

        // labelers
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `labelers_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `creatorId` TEXT NOT NULL,
                `likeCount` INTEGER,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO labelers_new SELECT * FROM labelers")
        connection.execSQL("DROP TABLE labelers")
        connection.execSQL("ALTER TABLE labelers_new RENAME TO labelers")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labelers_uri` ON `labelers` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labelers_creatorId` ON `labelers` (`creatorId`)")

        // labelDefinitions
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `labelDefinitions_new` (
                `creatorId` TEXT NOT NULL,
                `identifier` TEXT NOT NULL,
                `adultOnly` INTEGER NOT NULL,
                `blurs` TEXT NOT NULL,
                `defaultSetting` TEXT NOT NULL,
                `severity` TEXT NOT NULL,
                `localeInfoCbor` TEXT NOT NULL,
                PRIMARY KEY(`creatorId`, `identifier`),
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO labelDefinitions_new SELECT * FROM labelDefinitions")
        connection.execSQL("DROP TABLE labelDefinitions")
        connection.execSQL("ALTER TABLE labelDefinitions_new RENAME TO labelDefinitions")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labelDefinitions_creatorId` ON `labelDefinitions` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_labelDefinitions_identifier` ON `labelDefinitions` (`identifier`)")

        // lists
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
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO lists_new SELECT * FROM lists")
        connection.execSQL("DROP TABLE lists")
        connection.execSQL("ALTER TABLE lists_new RENAME TO lists")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_uri` ON `lists` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_cid` ON `lists` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_creatorId` ON `lists` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_indexedAt` ON `lists` (`indexedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_createdAt` ON `lists` (`createdAt`)")

        // listMembers
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `listMembers_new` (
                `uri` TEXT NOT NULL,
                `listUri` TEXT NOT NULL,
                `subjectId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`subjectId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`listUri`) REFERENCES `lists`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO listMembers_new SELECT * FROM listMembers")
        connection.execSQL("DROP TABLE listMembers")
        connection.execSQL("ALTER TABLE listMembers_new RENAME TO listMembers")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_listMembers_createdAt` ON `listMembers` (`createdAt`)")

        // feedGenerators
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
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO feedGenerators_new SELECT * FROM feedGenerators")
        connection.execSQL("DROP TABLE feedGenerators")
        connection.execSQL("ALTER TABLE feedGenerators_new RENAME TO feedGenerators")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_feedGenerators_uri` ON `feedGenerators` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_feedGenerators_cid` ON `feedGenerators` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_feedGenerators_creatorId` ON `feedGenerators` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_feedGenerators_indexedAt` ON `feedGenerators` (`indexedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_feedGenerators_createdAt` ON `feedGenerators` (`createdAt`)")

        // notifications
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
                FOREIGN KEY(`associatedPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`authorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`ownerId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO notifications_new SELECT * FROM notifications")
        connection.execSQL("DROP TABLE notifications")
        connection.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_uri` ON `notifications` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_cid` ON `notifications` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_authorId` ON `notifications` (`authorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_ownerId` ON `notifications` (`ownerId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_indexedAt` ON `notifications` (`indexedAt`)")

        // timelineItems
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `timelineItems_new` (
                `postUri` TEXT NOT NULL,
                `viewingProfileId` TEXT,
                `sourceId` TEXT NOT NULL,
                `embeddedRecordUri` TEXT,
                `reposter` TEXT,
                `hasMedia` INTEGER NOT NULL DEFAULT false,
                `isPinned` INTEGER NOT NULL,
                `itemSort` INTEGER NOT NULL,
                `indexedAt` INTEGER NOT NULL,
                `id` TEXT NOT NULL,
                `rootPostUri` TEXT,
                `rootPostEmbeddedRecordUri` TEXT DEFAULT NULL,
                `parentPostUri` TEXT,
                `parentPostEmbeddedRecordUri` TEXT DEFAULT NULL,
                `grandParentPostAuthorId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`rootPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`parentPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`grandParentPostAuthorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO timelineItems_new SELECT * FROM timelineItems")
        connection.execSQL("DROP TABLE timelineItems")
        connection.execSQL("ALTER TABLE timelineItems_new RENAME TO timelineItems")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_postUri` ON `timelineItems` (`postUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_indexedAt` ON `timelineItems` (`indexedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_viewingProfileId` ON `timelineItems` (`viewingProfileId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_sourceId` ON `timelineItems` (`sourceId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_rootPostUri` ON `timelineItems` (`rootPostUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_parentPostUri` ON `timelineItems` (`parentPostUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_grandParentPostAuthorId` ON `timelineItems` (`grandParentPostAuthorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_timelineItems_itemSort` ON `timelineItems` (`itemSort`)")

        // starterPacks
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
                FOREIGN KEY(`creatorId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO starterPacks_new SELECT * FROM starterPacks")
        connection.execSQL("DROP TABLE starterPacks")
        connection.execSQL("ALTER TABLE starterPacks_new RENAME TO starterPacks")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_starterPacks_uri` ON `starterPacks` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_starterPacks_cid` ON `starterPacks` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_starterPacks_creatorId` ON `starterPacks` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_starterPacks_indexedAt` ON `starterPacks` (`indexedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_starterPacks_createdAt` ON `starterPacks` (`createdAt`)")

        // conversations
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
                FOREIGN KEY(`ownerId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO conversations_new SELECT * FROM conversations")
        connection.execSQL("DROP TABLE conversations")
        connection.execSQL("ALTER TABLE conversations_new RENAME TO conversations")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_id` ON `conversations` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_ownerId` ON `conversations` (`ownerId`)")

        // conversationMembers
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversationMembers_new` (
                `conversationId` TEXT NOT NULL,
                `conversationOwnerId` TEXT NOT NULL,
                `memberId` TEXT NOT NULL,
                PRIMARY KEY(`conversationId`, `memberId`),
                FOREIGN KEY(`conversationId`, `conversationOwnerId`) REFERENCES `conversations`(`id`, `ownerId`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`memberId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO conversationMembers_new SELECT * FROM conversationMembers")
        connection.execSQL("DROP TABLE conversationMembers")
        connection.execSQL("ALTER TABLE conversationMembers_new RENAME TO conversationMembers")

        // messages
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
                `base64EncodedMetadata` TEXT DEFAULT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`conversationId`, `conversationOwnerId`) REFERENCES `conversations`(`id`, `ownerId`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`senderId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messages_new SELECT * FROM messages")
        connection.execSQL("DROP TABLE messages")
        connection.execSQL("ALTER TABLE messages_new RENAME TO messages")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_sentAt` ON `messages` (`sentAt`)")

        // messageFeedGenerators
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messageFeedGenerators_new` (
                `messageId` TEXT NOT NULL,
                `feedGeneratorUri` TEXT NOT NULL,
                PRIMARY KEY(`messageId`, `feedGeneratorUri`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`feedGeneratorUri`) REFERENCES `feedGenerators`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messageFeedGenerators_new SELECT * FROM messageFeedGenerators")
        connection.execSQL("DROP TABLE messageFeedGenerators")
        connection.execSQL("ALTER TABLE messageFeedGenerators_new RENAME TO messageFeedGenerators")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageFeedGenerators_messageId` ON `messageFeedGenerators` (`messageId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageFeedGenerators_feedGeneratorUri` ON `messageFeedGenerators` (`feedGeneratorUri`)")

        // messageLists
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messageLists_new` (
                `messageId` TEXT NOT NULL,
                `listUri` TEXT NOT NULL,
                PRIMARY KEY(`messageId`, `listUri`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`listUri`) REFERENCES `lists`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messageLists_new SELECT * FROM messageLists")
        connection.execSQL("DROP TABLE messageLists")
        connection.execSQL("ALTER TABLE messageLists_new RENAME TO messageLists")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageLists_messageId` ON `messageLists` (`messageId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageLists_listUri` ON `messageLists` (`listUri`)")

        // messagePosts
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messagePosts_new` (
                `messageId` TEXT NOT NULL,
                `postUri` TEXT NOT NULL,
                PRIMARY KEY(`messageId`, `postUri`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`postUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messagePosts_new SELECT * FROM messagePosts")
        connection.execSQL("DROP TABLE messagePosts")
        connection.execSQL("ALTER TABLE messagePosts_new RENAME TO messagePosts")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messagePosts_messageId` ON `messagePosts` (`messageId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messagePosts_postUri` ON `messagePosts` (`postUri`)")

        // messageReactions
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messageReactions_new` (
                `value` TEXT NOT NULL,
                `messageId` TEXT NOT NULL,
                `senderId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`value`, `messageId`, `senderId`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`senderId`) REFERENCES `profiles`(`did`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messageReactions_new SELECT * FROM messageReactions")
        connection.execSQL("DROP TABLE messageReactions")
        connection.execSQL("ALTER TABLE messageReactions_new RENAME TO messageReactions")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageReactions_createdAt` ON `messageReactions` (`createdAt`)")

        // messageStarterPacks
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messageStarterPacks_new` (
                `messageId` TEXT NOT NULL,
                `starterPackUri` TEXT NOT NULL,
                PRIMARY KEY(`messageId`, `starterPackUri`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`starterPackUri`) REFERENCES `starterPacks`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO messageStarterPacks_new SELECT * FROM messageStarterPacks")
        connection.execSQL("DROP TABLE messageStarterPacks")
        connection.execSQL("ALTER TABLE messageStarterPacks_new RENAME TO messageStarterPacks")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageStarterPacks_messageId` ON `messageStarterPacks` (`messageId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messageStarterPacks_starterPackUri` ON `messageStarterPacks` (`starterPackUri`)")

        // threadGates
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `threadGates_new` (
                `cid` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `gatedPostUri` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `allowsFollowing` INTEGER,
                `allowsFollowers` INTEGER,
                `allowsMentioned` INTEGER,
                PRIMARY KEY(`uri`),
                FOREIGN KEY(`gatedPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO threadGates_new SELECT * FROM threadGates")
        connection.execSQL("DROP TABLE threadGates")
        connection.execSQL("ALTER TABLE threadGates_new RENAME TO threadGates")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGates_uri` ON `threadGates` (`uri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGates_cid` ON `threadGates` (`cid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGates_gatedPostUri` ON `threadGates` (`gatedPostUri`)")

        // threadGateAllowedLists
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `threadGateAllowedLists_new` (
                `threadGateUri` TEXT NOT NULL,
                `allowedListUri` TEXT NOT NULL,
                PRIMARY KEY(`threadGateUri`, `allowedListUri`),
                FOREIGN KEY(`threadGateUri`) REFERENCES `threadGates`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`allowedListUri`) REFERENCES `lists`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO threadGateAllowedLists_new SELECT * FROM threadGateAllowedLists")
        connection.execSQL("DROP TABLE threadGateAllowedLists")
        connection.execSQL("ALTER TABLE threadGateAllowedLists_new RENAME TO threadGateAllowedLists")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGateAllowedLists_threadGateUri` ON `threadGateAllowedLists` (`threadGateUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGateAllowedLists_allowedListUri` ON `threadGateAllowedLists` (`allowedListUri`)")

        // threadGateHiddenPosts
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `threadGateHiddenPosts_new` (
                `threadGateUri` TEXT NOT NULL,
                `hiddenPostUri` TEXT NOT NULL,
                PRIMARY KEY(`threadGateUri`, `hiddenPostUri`),
                FOREIGN KEY(`threadGateUri`) REFERENCES `threadGates`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE,
                FOREIGN KEY(`hiddenPostUri`) REFERENCES `posts`(`uri`) ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("INSERT INTO threadGateHiddenPosts_new SELECT * FROM threadGateHiddenPosts")
        connection.execSQL("DROP TABLE threadGateHiddenPosts")
        connection.execSQL("ALTER TABLE threadGateHiddenPosts_new RENAME TO threadGateHiddenPosts")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGateHiddenPosts_threadGateUri` ON `threadGateHiddenPosts` (`threadGateUri`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_threadGateHiddenPosts_hiddenPostUri` ON `threadGateHiddenPosts` (`hiddenPostUri`)")
    }
}
