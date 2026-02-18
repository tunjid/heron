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
import com.tunjid.heron.data.core.models.Constants

internal object Migration5To6NonNullPostUriAndAuthor : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Add Unknown user to the db
        connection.execSQL(
            """
           INSERT INTO profiles (did, handle, displayName, description, avatar, banner, followersCount, followsCount, postsCount, joinedViaStarterPack, indexedAt, createdAt)
           VALUES ('${Constants.UNKNOWN}', '${Constants.UNKNOWN}', '', '', NULL, NULL, 0, 0, 0, NULL, 0, 0);
            """
                .trimIndent()
        )

        connection.execSQL(
            """
              CREATE TABLE IF NOT EXISTS posts_new (
              cid TEXT NOT NULL,
              uri TEXT NOT NULL,
              authorId TEXT NOT NULL,
              replyCount INTEGER,
              repostCount INTEGER,
              likeCount INTEGER,
              quoteCount INTEGER,
              indexedAt INTEGER NOT NULL,
              text TEXT,
              base64EncodedRecord TEXT,
              createdAt INTEGER,
              PRIMARY KEY(cid)
            )
            """
                .trimIndent()
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
                COALESCE(uri, '${Constants.UNKNOWN}'),
                COALESCE(authorId, '${Constants.UNKNOWN}'),
                replyCount,
                repostCount,
                likeCount,
                quoteCount,
                indexedAt,
                text,
                base64EncodedRecord,
                createdAt
                FROM posts
            """
                .trimIndent()
        )

        // Remove the old table
        connection.execSQL("DROP TABLE posts")

        // Change the table name to the correct one
        connection.execSQL("ALTER TABLE posts_new RENAME TO posts")
    }
}
