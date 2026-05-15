package com.tunjid.heron.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration42To43RockskyCreatorId : Migration(42, 43) {

    // Adds creatorId column to all rocksky tables to scope
    // albums, artists, tracks and scrobbles per local profile.
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE rockskyAlbums ADD COLUMN `creatorId` TEXT NOT NULL DEFAULT ''",
        )
        connection.execSQL(
            "ALTER TABLE rockskyArtists ADD COLUMN `creatorId` TEXT NOT NULL DEFAULT ''",
        )
        connection.execSQL(
            "ALTER TABLE rockskyTracks ADD COLUMN `creatorId` TEXT NOT NULL DEFAULT ''",
        )
        connection.execSQL(
            "ALTER TABLE rockskyScrobbles ADD COLUMN `creatorId` TEXT NOT NULL DEFAULT ''",
        )

        // Index creatorId on each table since all read queries filter by it
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_rockskyAlbums_creatorId` ON `rockskyAlbums` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_rockskyArtists_creatorId` ON `rockskyArtists` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_rockskyTracks_creatorId` ON `rockskyTracks` (`creatorId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_rockskyScrobbles_creatorId` ON `rockskyScrobbles` (`creatorId`)")
    }
}
