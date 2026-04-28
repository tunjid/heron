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

package com.tunjid.heron.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileTab
import heron.ui.profile.generated.resources.Res
import heron.ui.profile.generated.resources.tab_documents
import heron.ui.profile.generated.resources.tab_feeds
import heron.ui.profile.generated.resources.tab_likes
import heron.ui.profile.generated.resources.tab_lists
import heron.ui.profile.generated.resources.tab_media
import heron.ui.profile.generated.resources.tab_posts
import heron.ui.profile.generated.resources.tab_publications
import heron.ui.profile.generated.resources.tab_replies
import heron.ui.profile.generated.resources.tab_starter_packs
import heron.ui.profile.generated.resources.tab_video

fun Modifier.profileBioTabBackground(
    color: () -> Color,
) = background(
    color = color(),
    shape = ProfileBioBackgroundShape,
)
    .fillMaxWidth()
    .height(ProfileBioTabHeight)

val ProfileTab.stringResource
    get() = when (this) {
        ProfileTab.Bluesky.Posts.Likes -> Res.string.tab_likes
        ProfileTab.Bluesky.Posts.Media -> Res.string.tab_media
        ProfileTab.Bluesky.Posts.Replies -> Res.string.tab_replies
        ProfileTab.Bluesky.Posts.Standard -> Res.string.tab_posts
        ProfileTab.Bluesky.Posts.Videos -> Res.string.tab_video
        is ProfileTab.Bluesky.FeedGenerators -> Res.string.tab_feeds
        is ProfileTab.Bluesky.Lists -> Res.string.tab_lists
        ProfileTab.Bluesky.StarterPacks -> Res.string.tab_starter_packs
        ProfileTab.StandardSite.Documents -> Res.string.tab_documents
        ProfileTab.StandardSite.Publications -> Res.string.tab_publications
    }

fun String.withProfileBannerSharedElementPrefix() = "banner-$this"
fun String.withProfileBioTabSharedElementPrefix() = "bio-tab-$this"
fun String.withProfileAvatarHaloSharedElementPrefix() = "avatar-halo-$this"
fun String.withProfileAvatarLiveSharedElementPrefix() = "avatar-live-$this"

fun Modifier.profileBannerSize() =
    heightIn(max = MaxBannerHeight)
        .fillMaxWidth()
        .aspectRatio(BannerAspectRatio)

val ProfileBioTabHeight = 32.dp
val MaxBannerHeight = 240.dp

const val BannerAspectRatio = 16f / 9
const val AvatarLiveZIndex = 7f
const val AvatarZIndex = 6f
const val AvatarHaloZIndex = 5f
const val SurfaceZIndex = 4f
const val BannerZIndex = 3f

private val ProfileBioBackgroundShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
)
