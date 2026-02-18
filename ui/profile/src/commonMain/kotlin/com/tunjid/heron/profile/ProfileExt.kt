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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.profileBioTabBackground(color: () -> Color) =
    background(color = color(), shape = ProfileBioBackgroundShape)
        .fillMaxWidth()
        .height(ProfileBioTabHeight)

fun String.withProfileBannerSharedElementPrefix() = "banner-$this"

fun String.withProfileBioTabSharedElementPrefix() = "bio-tab-$this"

fun String.withProfileAvatarHaloSharedElementPrefix() = "avatar-halo-$this"

val ProfileBioTabHeight = 32.dp
const val BannerAspectRatio = 16f / 9
const val AvatarZIndex = 6f
const val AvatarHaloZIndex = 5f
const val SurfaceZIndex = 4f
const val BannerZIndex = 3f

private val ProfileBioBackgroundShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
