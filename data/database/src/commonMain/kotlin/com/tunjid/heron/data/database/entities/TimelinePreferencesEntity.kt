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
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity.Partial.FetchedAt
import kotlin.time.Instant

@Entity(
    tableName = "timelinePreferences",
    indices = [Index(value = ["viewingProfileId"]), Index(value = ["sourceId"])],
)
data class TimelinePreferencesEntity(
    val sourceId: String,
    val viewingProfileId: ProfileId?,
    val lastFetchedAt: Instant,
    val preferredPresentation: String?,
    // Timeline items are unique to the profile viewing them
    @PrimaryKey
    val id: String = timelinePreferenceId(viewingProfileId = viewingProfileId, sourceId = sourceId),
) {
    sealed class Partial {
        abstract val id: String
        abstract val sourceId: String

        data class FetchedAt(
            override val id: String,
            override val sourceId: String,
            val lastFetchedAt: Instant,
        ) : Partial()

        data class PreferredPresentation(
            override val id: String,
            override val sourceId: String,
            val preferredPresentation: String?,
        ) : Partial()
    }
}

fun preferredPresentationPartial(
    signedInProfileId: ProfileId?,
    sourceId: String,
    presentation: Timeline.Presentation,
) =
    TimelinePreferencesEntity.Partial.PreferredPresentation(
        id = timelinePreferenceId(viewingProfileId = signedInProfileId, sourceId = sourceId),
        sourceId = sourceId,
        preferredPresentation = presentation.key,
    )

fun TimelinePreferencesEntity.fetchedAtPartial() =
    FetchedAt(id = id, sourceId = sourceId, lastFetchedAt = lastFetchedAt)

private fun timelinePreferenceId(viewingProfileId: ProfileId?, sourceId: String): String =
    "${viewingProfileId?.id}-$sourceId"
