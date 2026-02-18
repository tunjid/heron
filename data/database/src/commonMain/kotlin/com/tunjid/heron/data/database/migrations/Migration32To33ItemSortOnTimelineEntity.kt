package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration32To33ItemSortOnTimelineEntity : Migration(32, 33) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            ALTER TABLE timelineItems
            ADD COLUMN itemSort INTEGER NOT NULL DEFAULT 0
            """
                .trimIndent()
        )

        connection.execSQL(
            """
            UPDATE timelineItems SET itemSort = indexedAt
            """
                .trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_timelineItems_itemSort
            ON timelineItems (itemSort)
            """
                .trimIndent()
        )
    }
}
