package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration29To30PostBookmarkViewingProfileId : Migration(29, 30) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE bookmarks (
                bookmarkedUri TEXT NOT NULL,
                viewingProfileId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(bookmarkedUri, viewingProfileId)
            )
            """.trimIndent(),
        )

        // Note: This table is empty at the time of migration since bookmarks feature is new
        // No data loss occurs as there are no existing bookmarks to migrate
        connection.execSQL("DROP TABLE postBookmark")

        connection.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_createdAt ON bookmarks (createdAt)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookmarkedUri ON bookmarks (bookmarkedUri)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_viewingProfileId_createdAt ON bookmarks (viewingProfileId, createdAt)")
    }
}
