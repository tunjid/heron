package com.tunjid.heron.data.database

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.tunjid.heron.data.core.models.Constants

internal object NonNullPostUriAndAuthorMigration : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Add Unknown user to the db
        connection.execSQL(
            """
           INSERT INTO profiles (did, handle, displayName, description, avatar, banner, followersCount, followsCount, postsCount, joinedViaStarterPack, indexedAt, createdAt)
           VALUES ('${Constants.UNKNOWN}', '${Constants.UNKNOWN}', '', '', NULL, NULL, 0, 0, 0, NULL, 0, 0);
            """.trimIndent()
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
            """.trimIndent()
        )

        // Remove the old table
        connection.execSQL("DROP TABLE posts")

        // Change the table name to the correct one
        connection.execSQL("ALTER TABLE posts_new RENAME TO posts")
    }
}

@DeleteColumn(tableName = "postViewerStatistics", columnName = "liked")
@DeleteColumn(tableName = "postViewerStatistics", columnName = "reposted")
internal class PostViewerStatisticsAutoMigration : AutoMigrationSpec