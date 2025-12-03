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

package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant

@Entity(
    tableName = "bookmarks",
    primaryKeys = [
        "bookmarkedUri",
        "viewingProfileId",
    ],
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["bookmarkedUri"]),
        Index(value = ["viewingProfileId", "createdAt"]),
    ],
)
data class BookmarkEntity(
    val bookmarkedUri: GenericUri,
    val viewingProfileId: ProfileId,
    val createdAt: Instant,
)
