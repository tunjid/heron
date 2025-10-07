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

package com.tunjid.heron.editprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.edit_banner_icon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditProfileScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        EditProfileHeader(
            profile = profile,
            onBannerEditClick = { },
            onAvatarEditClick = { },
        )
    }
}

@Composable
fun EditProfileHeader(
    profile: Profile,
    onBannerEditClick: () -> Unit,
    onAvatarEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        ProfileBannerEditableImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.TopCenter),
            profile = profile,
            onEditClick = onBannerEditClick,
        )

        ProfileAvatarEditableImage(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = 40.dp)
                .zIndex(2f),
            profile = profile,
            shape = CircleShape,
            onEditClick = onAvatarEditClick,
            size = 96.dp,
        )
    }
}

@Composable
fun ProfileAvatarEditableImage(
    modifier: Modifier = Modifier,
    profile: Profile,
    shape: Shape,
    onEditClick: () -> Unit,
    size: Dp? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        AsyncImage(
            modifier = Modifier
                .then(if (size != null) Modifier.size(size) else Modifier)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            args = remember(
                key1 = profile.avatar?.uri,
                key2 = profile.displayName,
                key3 = profile.handle,
            ) {
                ImageArgs(
                    url = profile.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape = RoundedPolygonShape.Rectangle,
                )
            },
        )

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .offset(x = (-4).dp, y = (-4).dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .background(MaterialTheme.colorScheme.surfaceBright, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(Res.string.edit_banner_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun ProfileBannerEditableImage(
    modifier: Modifier = Modifier,
    profile: Profile,
    onEditClick: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            args = remember(
                key1 = profile.banner?.uri,
                key2 = profile.displayName,
                key3 = profile.handle,
            ) {
                ImageArgs(
                    url = profile.banner?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape = RoundedPolygonShape.Rectangle,
                )
            },
        )

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .padding(8.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .background(MaterialTheme.colorScheme.surfaceBright, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .size(30.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(Res.string.edit_banner_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
