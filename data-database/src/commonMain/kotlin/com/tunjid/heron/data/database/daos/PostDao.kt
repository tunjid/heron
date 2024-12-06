package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import com.tunjid.heron.data.database.entities.PostAuthorCrossRef
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedCrossRef
import com.tunjid.heron.data.database.entities.PostImageCrossRef
import com.tunjid.heron.data.database.entities.PostVideoCrossRef

@Dao
interface PostDao {

    @Upsert
    suspend fun upsertPosts(
        entities: List<PostEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreAuthorCrossRefEntities(
        crossReferences: List<PostAuthorCrossRef>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreExternalEmbedCrossRefEntities(
        crossReferences: List<PostExternalEmbedCrossRef>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreImageCrossRefEntities(
        crossReferences: List<PostImageCrossRef>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreVideoCrossRefEntities(
        crossReferences: List<PostVideoCrossRef>,
    )

}
