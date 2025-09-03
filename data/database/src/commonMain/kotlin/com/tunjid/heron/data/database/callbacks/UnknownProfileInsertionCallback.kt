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

package com.tunjid.heron.data.database.callbacks

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.tunjid.heron.data.core.models.Constants

internal object UnknownProfileInsertionCallback : RoomDatabase.Callback() {
    override fun onCreate(
        connection: SQLiteConnection,
    ) = connection.execSQL(
        """
        INSERT OR IGNORE INTO profiles (
            did,
            handle,
            displayName,
            description,
            avatar,
            banner,
            followersCount,
            followsCount,
            postsCount,
            joinedViaStarterPack,
            indexedAt,
            createdAt,
            createdListCount,
            createdFeedGeneratorCount,
            createdStarterPackCount,
            labeler,
            allowDms
        )
        VALUES (
            '${Constants.unknownAuthorId}',
            '${Constants.UNKNOWN}',
            '',
            '',
            NULL,
            NULL,
            0,
            0,
            0,
            NULL,
            0,
            0,
            NULL,
            NULL,
            NULL,
            NULL,
            NULL
       );
        """.trimIndent(),
    )
}
