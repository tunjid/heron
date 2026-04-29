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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.editprofile.ui.EditButton
import com.tunjid.heron.editprofile.ui.EditProfileTabs
import com.tunjid.heron.editprofile.ui.TabEditor
import com.tunjid.heron.editprofile.ui.rememberEditProfileTabs
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.media.picker.MediaType
import com.tunjid.heron.media.picker.rememberMediaPicker
import com.tunjid.heron.profile.AvatarHaloZIndex
import com.tunjid.heron.profile.AvatarZIndex
import com.tunjid.heron.profile.BannerZIndex
import com.tunjid.heron.profile.SurfaceZIndex
import com.tunjid.heron.profile.profileBannerSize
import com.tunjid.heron.profile.profileBioTabBackground
import com.tunjid.heron.profile.withProfileAvatarHaloSharedElementPrefix
import com.tunjid.heron.profile.withProfileBannerSharedElementPrefix
import com.tunjid.heron.profile.withProfileBioTabSharedElementPrefix
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.record.RecordList
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.FormField
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf

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

    val density = LocalDensity.current

    val collapsedHeight = UiTokens.toolbarHeight + UiTokens.statusBarHeight

    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = with(density) { collapsedHeight.toPx() },
        initialExpandedHeight = with(density) { 800.dp.toPx() },
    )
    collapsingHeaderState.headerZIndex = 1f

    Box(
        modifier = modifier,
    ) {
        ProfileBannerEditableImage(
            modifier = Modifier
                .profileBannerSize()
                .align(Alignment.TopCenter),
            paneScaffoldState = paneScaffoldState,
            headerState = collapsingHeaderState,
            avatarSharedElementKey = state.avatarSharedElementKey,
            profile = state.profile,
            localFile = state.updatedBanner,
        )
        CollapsingHeaderLayout(
            modifier = modifier,
            state = collapsingHeaderState,
            headerContent = {
                EditProfileHeader(
                    modifier = Modifier
                        .zIndex(1f),
                    paneScaffoldState = paneScaffoldState,
                    headerState = collapsingHeaderState,
                    avatarSharedElementKey = state.avatarSharedElementKey,
                    profile = state.profile,
                    onBannerEditClick = { bannerPicker() },
                    onAvatarEditClick = { avatarPicker() },
                    avatarFile = state.updatedAvatar,
                )
            },
            body = {
                val surfaceColor = MaterialTheme.colorScheme.surface
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    val bioTabColorState = animateColorAsState(
                        if (paneScaffoldState.inPredictiveBack) Color.Transparent
                        else surfaceColor,
                    )
                    with(paneScaffoldState) {
                        PaneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = state.avatarSharedElementKey.withProfileBioTabSharedElementPrefix(),
                            ),
                            zIndexInOverlay = SurfaceZIndex,
                        ) {
                            Box(
                                Modifier
                                    .zIndex(0f)
                                    .profileBioTabBackground(bioTabColorState::value)
                                    .fillParentAxisIfFixedOrWrap(),
                            )
                        }
                    }
                    val updatedTabs by rememberUpdatedState(state.tabs)
                    val pagerState = rememberPagerState { updatedTabs.size }
                    Column(
                        modifier = Modifier
                            .background(surfaceColor),
                    ) {
                        EditProfileTabs(
                            modifier = Modifier
                                .screenHorizontalPadding()
                                .padding(vertical = 24.dp),
                            pagerState = pagerState,
                            tabs = rememberEditProfileTabs(updatedTabs),
                        )
                        HorizontalPager(
                            state = pagerState,
                            pageContent = { page ->
                                when (val tab = updatedTabs[page]) {
                                    EditProfileScreenTabs.Bio -> ProfileBio(
                                        modifier = Modifier
                                            .screenHorizontalPadding(),
                                        state = state,
                                        actions = actions,
                                    )
                                    EditProfileScreenTabs.Editor -> TabEditor(
                                        modifier = Modifier
                                            .screenHorizontalPadding(),
                                        editableTabs = state.editableTabs,
                                        currentTabs = state.currentProfileTabs,
                                        feedUrisToFeeds = state.feedUrisToFeeds,
                                        onPinnedTabsChanged = {
                                            actions(Action.UpdateTabsToSave(it))
                                        },
                                    )
                                    is EditProfileScreenTabs.Feeds -> RecordList(
                                        collectionStateHolder = tab,
                                        prefersCompactBottomNav = paneScaffoldState.prefersCompactBottomNav,
                                        itemKey = { it.uri.uri },
                                        itemContent = { feedGenerator ->
                                            Row(
                                                modifier = Modifier
                                                    .fillParentMaxWidth()
                                                    .animateItem()
                                                    .shapedClickable {
                                                        actions(
                                                            Action.ToggleFeed(feedGenerator),
                                                        )
                                                    }
                                                    .padding(all = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                FeedGenerator(
                                                    modifier = Modifier
                                                        .weight(1f),
                                                    paneTransitionScope = paneScaffoldState,
                                                    sharedElementPrefix = "",
                                                    feedGenerator = feedGenerator,
                                                    status = null,
                                                    onFeedGeneratorStatusUpdated = {
                                                        // No-op status is always null
                                                    },
                                                )
                                                Checkbox(
                                                    checked = state.feedUrisToFeeds.contains(
                                                        feedGenerator.uri,
                                                    ),
                                                    onCheckedChange = {
                                                        actions(
                                                            Action.ToggleFeed(feedGenerator),
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ProfileBio(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
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

@Composable
fun EditProfileHeader(
    paneScaffoldState: PaneScaffoldState,
    headerState: CollapsingHeaderState,
    avatarSharedElementKey: String,
    avatarFile: RestrictedFile.Media.Photo?,
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
        Box(
            modifier = Modifier
                .profileBannerSize()
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(
                        fraction = 1f - headerState.progress,
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onBannerEditClick,
                    ),
            )
        }

        ProfileAvatarEditableImage(
            modifier = Modifier
                .align(
                    BiasAlignment(
                        horizontalBias = -1f + (headerState.progress * 2),
                        verticalBias = 1f - (headerState.progress * 2),
                    ),
                )
                .offset {
                    Offset(
                        x = lerp(
                            start = 16.dp,
                            stop = -(48).dp,
                            fraction = headerState.progress,
                        ).toPx(),
                        y = lerp(
                            start = 40.dp,
                            stop = 80.dp,
                            fraction = headerState.progress,
                        ).toPx(),
                    ).round()
                }
                .zIndex(2f),
            paneScaffoldState = paneScaffoldState,
            headerState = headerState,
            avatarSharedElementKey = avatarSharedElementKey,
            profile = profile,
            shape = CircleShape,
            onEditClick = onAvatarEditClick,
            size = lerp(
                start = 96.dp,
                stop = 30.dp,
                fraction = headerState.progress,
            ),
            localFile = avatarFile,
        )
    }
}

@Composable
fun ProfileAvatarEditableImage(
    paneScaffoldState: PaneScaffoldState,
    headerState: CollapsingHeaderState,
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
            .size(size)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onEditClick,
            ),
        contentAlignment = Alignment.BottomEnd,
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .matchParentSize(),
            sharedContentState = rememberSharedContentState(
                key = avatarSharedElementKey.withProfileAvatarHaloSharedElementPrefix(),
            ),
            zIndexInOverlay = AvatarHaloZIndex,
        ) {
            Box(
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                    )
                    .fillParentAxisIfFixedOrWrap(),
            )
        }
        paneScaffoldState.UpdatedMovableStickySharedElementOf(
            sharedContentState = with(paneScaffoldState) {
                rememberSharedContentState(
                    key = avatarSharedElementKey,
                )
            },
            zIndexInOverlay = AvatarZIndex,
            modifier = Modifier
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

        EditButton(
            modifier = Modifier
                .graphicsLayer {
                    alpha = 1f - headerState.progress
                }
                .offset(x = (-4).dp, y = (-4).dp)
                .shapedClickable(
                    shape = CircleShape,
                    onClick = onEditClick,
                ),
        )
    }
}

@Composable
fun ProfileBannerEditableImage(
    paneScaffoldState: PaneScaffoldState,
    headerState: CollapsingHeaderState,
    avatarSharedElementKey: String,
    profile: Profile,
    localFile: RestrictedFile.Media.Photo?,
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
        Box(
            modifier = Modifier
                .background(
                    color = lerp(
                        start = Color.Transparent,
                        stop = Color.Black.copy(alpha = 0.8f),
                        fraction = headerState.progress,
                    ),
                )
                .matchParentSize(),
        )
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

private fun Modifier.screenHorizontalPadding() =
    padding(
        horizontal = 16.dp,
    )
