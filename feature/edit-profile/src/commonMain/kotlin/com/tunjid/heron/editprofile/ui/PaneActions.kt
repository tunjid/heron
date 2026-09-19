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

package com.tunjid.heron.editprofile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.editprofile.Action
import com.tunjid.heron.media.picker.MediaType
import com.tunjid.heron.media.picker.rememberMediaPicker
import com.tunjid.heron.ui.AppBarIconButton
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.edit_banner_icon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneActions(
    actions: (Action) -> Unit,
) {
    val bannerPicker = rememberMediaPicker(
        mediaType = MediaType.Photo,
        maxItems = 1,
    ) { mediaItems ->
        mediaItems
            .filterIsInstance<RestrictedFile.Media.Photo>()
            .firstOrNull()
            ?.let { actions(Action.BannerPicked(it)) }
    }
    AppBarIconButton(
        icon = Icons.Rounded.Edit,
        iconDescription = stringResource(Res.string.edit_banner_icon),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
        onClick = { bannerPicker() },
    )
}
