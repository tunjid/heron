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

package com.tunjid.heron.data.database

import androidx.room.TypeConverter
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileUri
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

internal class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? =
        value?.let(Instant.Companion::fromEpochMilliseconds)

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? =
        instant?.toEpochMilliseconds()
}

internal class UriConverters {

    @TypeConverter
    fun fromString(value: String?): Uri? =
        value?.let(::GenericUri)

    @TypeConverter
    fun genericUriFromString(value: String?): GenericUri? =
        value?.let(::GenericUri)

    @TypeConverter
    fun postUriFromString(value: String?): PostUri? =
        value?.let(::PostUri)

    @TypeConverter
    fun profileUriFromString(value: String?): ProfileUri? =
        value?.let(::ProfileUri)

    @TypeConverter
    fun feedGeneratorUriFromString(value: String?): FeedGeneratorUri? =
        value?.let(::FeedGeneratorUri)

    @TypeConverter
    fun listUriFromString(value: String?): ListUri? =
        value?.let(::ListUri)

    @TypeConverter
    fun listMemberUriFromString(value: String?): ListMemberUri? =
        value?.let(::ListMemberUri)

    @TypeConverter
    fun imageUriFromString(value: String?): ImageUri? =
        value?.let(::ImageUri)

    @TypeConverter
    fun toUriString(uri: Uri?): String? =
        uri?.uri

}

internal class IdConverters {
    @TypeConverter
    fun fromString(value: String?): Id? =
        value?.let(::GenericId)

    @TypeConverter
    fun genericIdFromString(value: String?): GenericId? =
        value?.let(::GenericId)

    @TypeConverter
    fun postIdFromString(value: String?): PostId? =
        value?.let(::PostId)

    @TypeConverter
    fun profileIdFromString(value: String?): ProfileId? =
        value?.let(::ProfileId)

    @TypeConverter
    fun profileHandleFromString(value: String?): ProfileHandle? =
        value?.let(::ProfileHandle)

    @TypeConverter
    fun listIdFromString(value: String?): ListId? =
        value?.let(::ListId)

    @TypeConverter
    fun feedGeneratorIdFromString(value: String?): FeedGeneratorId? =
        value?.let(::FeedGeneratorId)

    @TypeConverter
    fun toUriString(id: Id?): String? =
        id?.id
}
