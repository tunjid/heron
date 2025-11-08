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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.picker.MediaType
import com.tunjid.heron.media.picker.rememberMediaPicker
import com.tunjid.heron.profile.AvatarHaloZIndex
import com.tunjid.heron.profile.AvatarZIndex
import com.tunjid.heron.profile.BannerAspectRatio
import com.tunjid.heron.profile.BannerZIndex
import com.tunjid.heron.profile.SurfaceZIndex
import com.tunjid.heron.profile.profileBioTabBackground
import com.tunjid.heron.profile.withProfileAvatarHaloSharedElementPrefix
import com.tunjid.heron.profile.withProfileBannerSharedElementPrefix
import com.tunjid.heron.profile.withProfileBioTabSharedElementPrefix
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.FormField
import com.tunjid.treenav.compose.moveablesharedelement.UpdatedMovableStickySharedElementOf
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.edit_avatar_icon
import heron.feature.edit_profile.generated.resources.edit_banner_icon
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun EditProfileScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarPicker = rememberMediaPicker(
        mediaType = MediaType.Photo,
        maxItems = 1,
    ) { mediaItems ->
        mediaItems
            .filterIsInstance<RestrictedFile.Media.Photo>()
            .firstOrNull()
            ?.let { actions(Action.AvatarPicked(it)) }
    }

    val bannerPicker = rememberMediaPicker(
        mediaType = MediaType.Photo,
        maxItems = 1,
    ) { mediaItems ->
        mediaItems
            .filterIsInstance<RestrictedFile.Media.Photo>()
            .firstOrNull()
            ?.let { actions(Action.BannerPicked(it)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
    ) {
        EditProfileHeader(
            modifier = Modifier
                .zIndex(1f),
            paneScaffoldState = paneScaffoldState,
            avatarSharedElementKey = state.avatarSharedElementKey,
            profile = state.profile,
            onBannerEditClick = { bannerPicker() },
            onAvatarEditClick = { avatarPicker() },
            avatarFile = state.updatedAvatar,
            bannerFile = state.updatedBanner,
        )
        val surfaceColor = MaterialTheme.colorScheme.surface
        val bioTabColorState = animateColorAsState(
            if (paneScaffoldState.inPredictiveBack) Color.Transparent
            else surfaceColor,
        )
        val focusManager = LocalFocusManager.current
        Box(
            modifier = with(paneScaffoldState) {
                Modifier
                    .zIndex(0f)
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = state.avatarSharedElementKey.withProfileBioTabSharedElementPrefix(),
                        ),
                        zIndexInOverlay = SurfaceZIndex,
                    )
                    .profileBioTabBackground(bioTabColorState::value)
            },
        )
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            state.fields.forEach { field ->
                key(field.id) {
                    FormField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        field = field,
                        onValueChange = { field, newValue ->
                            actions(
                                Action.FieldChanged(
                                    id = field.id,
                                    text = newValue,
                                ),
                            )
                        },
                        keyboardActions = {
                            when (it.id) {
                                DisplayName -> focusManager.moveFocus(
                                    focusDirection = FocusDirection.Next,
                                )

                                Description -> actions(
                                    state.saveProfileAction(),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun EditProfileHeader(
    paneScaffoldState: PaneScaffoldState,
    avatarSharedElementKey: String,
    avatarFile: RestrictedFile.Media.Photo?,
    bannerFile: RestrictedFile.Media.Photo?,
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
                .aspectRatio(BannerAspectRatio)
                .align(Alignment.TopCenter),
            paneScaffoldState = paneScaffoldState,
            avatarSharedElementKey = avatarSharedElementKey,
            profile = profile,
            onEditClick = onBannerEditClick,
            localFile = bannerFile,
        )

        ProfileAvatarEditableImage(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = 40.dp)
                .zIndex(2f),
            paneScaffoldState = paneScaffoldState,
            avatarSharedElementKey = avatarSharedElementKey,
            profile = profile,
            shape = CircleShape,
            onEditClick = onAvatarEditClick,
            size = 96.dp,
            localFile = avatarFile,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileAvatarEditableImage(
    paneScaffoldState: PaneScaffoldState,
    avatarSharedElementKey: String,
    profile: Profile,
    localFile: RestrictedFile.Media.Photo?,
    shape: Shape,
    size: Dp,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
) = with(paneScaffoldState) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .paneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = avatarSharedElementKey.withProfileAvatarHaloSharedElementPrefix(),
                    ),
                    zIndexInOverlay = AvatarHaloZIndex,
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                )
                .matchParentSize(),
        )
        paneScaffoldState.UpdatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = avatarSharedElementKey,
                )
            },
            zIndexInOverlay = AvatarZIndex,
            modifier = Modifier
                .padding(4.dp)
                .matchParentSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            state = rememberEditableImageArgs(
                profile = profile,
                localFile = localFile,
                remoteUri = profile.avatar?.uri,
                shape = RoundedPolygonShape.Circle,
            ),
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
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
                contentDescription = stringResource(Res.string.edit_avatar_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileBannerEditableImage(
    paneScaffoldState: PaneScaffoldState,
    avatarSharedElementKey: String,
    profile: Profile,
    localFile: RestrictedFile.Media.Photo?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        paneScaffoldState.UpdatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = avatarSharedElementKey.withProfileBannerSharedElementPrefix(),
                )
            },
            zIndexInOverlay = BannerZIndex,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            state = rememberEditableImageArgs(
                profile = profile,
                localFile = localFile,
                remoteUri = profile.banner?.uri,
                shape = RoundedPolygonShape.Rectangle,
            ),
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
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

@Composable
private fun rememberEditableImageArgs(
    profile: Profile,
    localFile: RestrictedFile.Media.Photo?,
    remoteUri: String?,
    shape: RoundedPolygonShape,
): ImageArgs {
    return remember(localFile?.path, remoteUri) {
        val contentDescription = profile.displayName ?: profile.handle.id
        if (localFile != null)
            ImageArgs(
                item = localFile,
                contentScale = ContentScale.Crop,
                contentDescription = contentDescription,
                shape = shape,
            )
        else
            ImageArgs(
                url = remoteUri,
                contentScale = ContentScale.Crop,
                contentDescription = contentDescription,
                shape = shape,
            )
    }
}
