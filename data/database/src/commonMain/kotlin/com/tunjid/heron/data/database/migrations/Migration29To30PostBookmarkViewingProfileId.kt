package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration29To30PostBookmarkViewingProfileId : Migration(28, 29) {
    override fun migrate(connection: SQLiteConnection) {
        // Note: This table is empty at the time of migration since bookmarks feature is new
        // No data loss occurs as there are no existing bookmarks to migrate

        connection.execSQL(
            """
            CREATE TABLE postBookmarks (
                viewingProfileId TEXT NOT NULL,
                postUri TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(viewingProfileId, postUri),
                FOREIGN KEY(postUri) REFERENCES posts(uri) ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        connection.execSQL("CREATE INDEX IF NOT EXISTS index_postBookmarks_createdAt ON postBookmarks (createdAt)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_postBookmarks_viewingProfileId_createdAt ON postBookmarks (viewingProfileId, createdAt)")
    }
}
