package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL


internal object Migration34To35ProfileStatus : Migration(34, 35) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_uri TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_value TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_uriLink TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_title TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_description TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_thumbnail TEXT;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_expiresAt INTEGER;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_active INTEGER;")
        connection.execSQL("ALTER TABLE profiles ADD COLUMN live_disabled INTEGER;")
    }
}
