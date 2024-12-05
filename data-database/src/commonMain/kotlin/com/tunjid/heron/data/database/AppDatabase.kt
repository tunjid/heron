package com.tunjid.heron.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedCrossRef
import com.tunjid.heron.data.database.entities.PostImageCrossRef
import com.tunjid.heron.data.database.entities.PostVideoCrossRef
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.VideoEntity

@Database(
    version = 1,
    entities = [
        ExternalEmbedEntity::class,
        ImageEntity::class,
        VideoEntity::class,
        PostExternalEmbedCrossRef::class,
        PostImageCrossRef::class,
        PostVideoCrossRef::class,
        PostEntity::class,
        ProfileEntity::class,
    ],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}