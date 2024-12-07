package com.tunjid.heron.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.FeedItemEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.PostImageEntity
import com.tunjid.heron.data.database.entities.PostVideoEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.VideoEntity

@Database(
    version = 1,
    entities = [
        ExternalEmbedEntity::class,
        ImageEntity::class,
        VideoEntity::class,
        PostExternalEmbedEntity::class,
        PostImageEntity::class,
        PostVideoEntity::class,
        PostEntity::class,
        ProfileEntity::class,
        FeedItemEntity::class,
        PostAuthorsEntity::class,
    ],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun embedDao(): EmbedDao
    abstract fun feedDao(): FeedDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}