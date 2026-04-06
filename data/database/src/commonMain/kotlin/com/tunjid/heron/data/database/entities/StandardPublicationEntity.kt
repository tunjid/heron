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

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import kotlin.time.Instant

@Entity(
    tableName = "standardPublications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["publisherId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"]),
        Index(value = ["publisherId"]),
        Index(value = ["sortedAt"]),
    ],
)
data class StandardPublicationEntity(
    @PrimaryKey
    val uri: StandardPublicationUri,
    val cid: StandardPublicationId?,
    val publisherId: ProfileId,
    val name: String,
    val description: String?,
    val url: String,
    val icon: ImageUri?,
    @Embedded(prefix = "preferences_")
    val preferences: Preferences?,
    @Embedded(prefix = "theme_")
    val basicTheme: BasicTheme?,
    @ColumnInfo(defaultValue = "0")
    val sortedAt: Instant,
) {
    data class Preferences(
        val showInDiscover: Boolean,
    )

    data class BasicTheme(
        @Embedded(prefix = "accent_")
        val accent: Color?,
        @Embedded(prefix = "accentFg_")
        val accentForeground: Color?,
        @Embedded(prefix = "bg_")
        val background: Color?,
        @Embedded(prefix = "fg_")
        val foreground: Color?,
    )

    data class Color(
        val r: Int,
        val g: Int,
        val b: Int,
        val a: Int,
    )
}

data class PopulatedStandardPublicationEntity(
    @Embedded
    val entity: StandardPublicationEntity,
    @Embedded(prefix = "publisher_")
    val publisher: ProfileEntity,
    @Embedded(prefix = "subscription_")
    val subscription: StandardSubscriptionEntity?,
)

fun StandardPublicationEntity.asExternalModel(
    publisher: Profile,
    subscription: StandardSubscription?,
) = StandardPublication(
    uri = uri,
    cid = cid,
    publisher = publisher,
    name = name,
    description = description,
    url = url,
    icon = icon,
    showInDiscover = preferences?.showInDiscover ?: true,
    subscription = subscription,
    basicTheme = basicTheme?.let { theme ->
        StandardPublication.BasicTheme(
            accent = theme.accent.asThemeColor() ?: return@let null,
            accentForeground = theme.accentForeground.asThemeColor() ?: return@let null,
            background = theme.background.asThemeColor() ?: return@let null,
            foreground = theme.foreground.asThemeColor() ?: return@let null,
        )
    },
)

fun PopulatedStandardPublicationEntity.asExternalModel() = entity.asExternalModel(
    publisher.asExternalModel(),
    subscription?.asExternalModel(),
)

private fun StandardPublicationEntity.Color?.asThemeColor(): StandardPublication.ThemeColor? =
    this?.let { StandardPublication.ThemeColor(r = it.r, g = it.g, b = it.b, a = it.a) }
