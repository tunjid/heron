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

package com.tunjid.heron.data.database.entities.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.ProfileEntity

@Entity(
    tableName = "profileTabs",
    primaryKeys = [
        "profileId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["did"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class ProfileTabsEntity(
    val profileId: ProfileId,
    val delimitedTabs: String,
)

fun ProfileTabsEntity?.asExternalModels(): List<ProfileTab> =
    when (this) {
        null -> Default
        else -> if (delimitedTabs.isBlank()) Default
        else delimitedTabs
            .split(TabDelimiter)
            .mapNotNull(String::asProfileTabOrNull)
            .ifEmpty { Default }
    }

private fun String.asProfileTabOrNull(): ProfileTab? = when (this) {
    Timeline.Profile.Type.Posts.suffix -> ProfileTab.Bluesky.Posts
    Timeline.Profile.Type.Replies.suffix -> ProfileTab.Bluesky.Replies
    Timeline.Profile.Type.Likes.suffix -> ProfileTab.Bluesky.Likes
    Timeline.Profile.Type.Media.suffix -> ProfileTab.Bluesky.Media
    Timeline.Profile.Type.Videos.suffix -> ProfileTab.Bluesky.Videos
    FeedGeneratorUri.NAMESPACE -> ProfileTab.Bluesky.FeedGenerators
    ListUri.NAMESPACE -> ProfileTab.Bluesky.Lists
    StarterPackUri.NAMESPACE -> ProfileTab.Bluesky.StarterPacks
    StandardPublicationUri.NAMESPACE -> ProfileTab.StandardSite.Publications
    StandardDocumentUri.NAMESPACE -> ProfileTab.StandardSite.Documents
    else -> null
}

fun List<ProfileTab>.profileTabsEntity(
    profileId: ProfileId,
) = ProfileTabsEntity(
    profileId = profileId,
    delimitedTabs = joinToString(
        separator = TabDelimiter,
        transform = { profileTab ->
            when (profileTab) {
                ProfileTab.Bluesky.Posts -> Timeline.Profile.Type.Posts.suffix
                ProfileTab.Bluesky.Replies -> Timeline.Profile.Type.Replies.suffix
                ProfileTab.Bluesky.Likes -> Timeline.Profile.Type.Likes.suffix
                ProfileTab.Bluesky.Media -> Timeline.Profile.Type.Media.suffix
                ProfileTab.Bluesky.Videos -> Timeline.Profile.Type.Videos.suffix
                ProfileTab.Bluesky.FeedGenerators -> FeedGeneratorUri.NAMESPACE
                ProfileTab.Bluesky.Lists -> ListUri.NAMESPACE
                ProfileTab.Bluesky.StarterPacks -> StarterPackUri.NAMESPACE
                ProfileTab.StandardSite.Documents -> StandardDocumentUri.NAMESPACE
                ProfileTab.StandardSite.Publications -> StandardPublicationUri.NAMESPACE
            }
        },
    ),
)
operator fun ProfileTabsEntity.plus(tab: String) = when (tab.asProfileTabOrNull()) {
    null -> this
    else -> copy(
        delimitedTabs = if (delimitedTabs.isBlank()) tab else "$delimitedTabs$TabDelimiter$tab"
    )
}

private val Default = listOf(
    ProfileTab.Bluesky.Posts,
    ProfileTab.Bluesky.Replies,
    ProfileTab.Bluesky.Likes,
    ProfileTab.Bluesky.Media,
    ProfileTab.Bluesky.Videos,
    ProfileTab.Bluesky.FeedGenerators,
    ProfileTab.StandardSite.Documents,
    ProfileTab.Bluesky.StarterPacks,
    ProfileTab.Bluesky.Lists,
)

private const val TabDelimiter = "@@@"
