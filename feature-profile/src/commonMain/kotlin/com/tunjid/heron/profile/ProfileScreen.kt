/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileRelationship
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.SharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.back
import heron.feature_profile.generated.resources.follow
import heron.feature_profile.generated.resources.followers
import heron.feature_profile.generated.resources.following
import heron.feature_profile.generated.resources.posts
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun ProfileScreen(
    sharedElementScope: SharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val collapsedHeight = with(density) { 56.dp.toPx() } +
            WindowInsets.statusBars.getTop(density).toFloat() +
            WindowInsets.statusBars.getBottom(density).toFloat()

    val headerState = remember {
        CollapsingHeaderState(
            collapsedHeight = collapsedHeight,
            initialExpandedHeight = with(density) { 800.dp.toPx() },
            decayAnimationSpec = splineBasedDecay(density)
        )
    }

    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.feed)

    CollapsingHeaderLayout(
        state = headerState,
        headerContent = {
            ProfileHeader(
                movableSharedElementScope = sharedElementScope,
                headerState = headerState,
                modifier = Modifier
                    .fillMaxWidth(),
                profile = state.profile,
                profileRelationship = state.profileRelationship,
                avatarSharedElementKey = state.avatarSharedElementKey,
                onBackPressed = {
                    actions(Action.Navigate.DelegateTo(NavigationAction.Common.Pop))
                }
            )
        },
        body = {
            Surface(
                modifier = modifier
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                )
            ) {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(340.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = items,
                        key = TimelineItem::id,
                        itemContent = { item ->
                            TimelineItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                                movableSharedElementScope = sharedElementScope,
                                animatedVisibilityScope = sharedElementScope,
                                now = remember { Clock.System.now() },
                                sharedElementPrefix = TimelineQuery.Profile.toString(),
                                item = item,
                                onPostClicked = { post ->
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToPost(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                sharedElementPrefix = TimelineQuery.Profile.toString(),
                                                post = post,
                                            )
                                        )
                                    )
                                },
                                onProfileClicked = { profile ->
                                    actions(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToProfile(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                                profile = profile,
                                                avatarSharedElementKey = this?.avatarSharedElementKey(
                                                    prefix = TimelineQuery.Profile.toString(),
                                                )
                                            )
                                        )
                                    )
                                },
                                onImageClicked = {},
                                onReplyToPost = {},
                            )
                        }
                    )
                }
            }
        }
    )


    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(Action.LoadFeed.LoadAround(query ?: state.currentQuery))
        }
    )
}

@Composable
private fun ProfileHeader(
    movableSharedElementScope: MovableSharedElementScope,
    headerState: CollapsingHeaderState,
    modifier: Modifier = Modifier,
    profile: Profile,
    profileRelationship: ProfileRelationship?,
    avatarSharedElementKey: String,
    onBackPressed: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ProfileBanner(
            modifier = Modifier
                .align(Alignment.TopCenter),
            headerState = headerState,
            profile = profile,
        )
        Column(
            modifier = modifier
                .padding(start = 24.dp, end = 24.dp, top = 160.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = -headerState.translation.roundToInt()
                    )
                }
        ) {
            Spacer(Modifier.height(24.dp))
            ProfileHeadline(
                modifier = Modifier.fillMaxWidth(),
                profile = profile,
                profileRelationship = profileRelationship,
            )
            ProfileStats(
                modifier = Modifier.fillMaxWidth(),
                profile = profile,
            )
            Text(text = profile.description ?: "")
            Spacer(Modifier.height(16.dp))
            // TODO Tabs
            Spacer(Modifier.height(48.dp))
        }
        ProfilePhoto(
            movableSharedElementScope = movableSharedElementScope,
            modifier = Modifier
                .align(Alignment.TopCenter),
            headerState = headerState,
            profile = profile,
            avatarSharedElementKey = avatarSharedElementKey,
        )
        BackButton(
            headerState = headerState,
            onBackPressed = onBackPressed,
        )
    }
}

@Composable
private fun ProfileBanner(
    modifier: Modifier = Modifier,
    headerState: CollapsingHeaderState,
    profile: Profile,
) {
    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                alpha = 1f - min(0.9f, (headerState.progress * 1.6f))
            },
        args = remember(
            key1 = profile.banner?.uri,
            key2 = profile.displayName,
            key3 = profile.handle,
        ) {
            ImageArgs(
                url = profile.banner?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = profile.displayName ?: profile.handle.id,
                shape = ImageShape.Rectangle,
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfilePhoto(
    modifier: Modifier = Modifier,
    movableSharedElementScope: MovableSharedElementScope,
    headerState: CollapsingHeaderState,
    profile: Profile,
    avatarSharedElementKey: String,
) {
    Card(
        modifier = modifier
            .padding(top = 124.dp)
            .size(68.dp)
            .offset {
                IntOffset(
                    x = 0,
                    y = -headerState.translation.roundToInt()
                )
            },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        movableSharedElementScope.updatedMovableSharedElementOf(
            key = avatarSharedElementKey,
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
            state = remember(
                key1 = profile.avatar?.uri,
                key2 = profile.displayName,
                key3 = profile.handle,
            ) {
                ImageArgs(
                    url = profile.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape = ImageShape.Circle,
                )
            },
            sharedElement = { state, modifier ->
                AsyncImage(state, modifier)
            }
        )
    }
}


@Composable
private fun ProfileHeadline(
    modifier: Modifier = Modifier,
    profile: Profile,
    profileRelationship: ProfileRelationship?,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            val primaryText = profile.displayName ?: profile.handle.id
            val secondaryText = profile.handle.id.takeUnless { it == primaryText }

            Text(
                modifier = Modifier,
                text = primaryText,
                maxLines = 1,
                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
            )
            if (secondaryText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    modifier = Modifier,
                    text = profile.handle.id,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        AnimatedVisibility(
            visible = profileRelationship != null,
            content = {
                val follows = profileRelationship?.follows == true
                val followStatusText = stringResource(
                    if (follows) Res.string.following
                    else Res.string.follow
                )
                FilterChip(
                    selected = follows,
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = if (follows) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = followStatusText,
                        )
                    },
                    label = {
                        Text(followStatusText)
                    },
                )
            },
        )
    }
}

@Composable
private fun ProfileStats(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Statistic(
            value = profile.followersCount ?: 0,
            description = stringResource(Res.string.followers),
            onClick = {}
        )
        Statistic(
            value = profile.followsCount ?: 0,
            description = stringResource(Res.string.following),
            onClick = {}
        )
        Statistic(
            value = profile.postsCount ?: 0,
            description = stringResource(Res.string.posts),
            onClick = {}
        )
    }
}

@Composable
fun Statistic(
    value: Long,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            modifier = Modifier,
            text = format(value),
            maxLines = 1,
            style = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            modifier = Modifier,
            text = description,
            maxLines = 1,
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun BackButton(
    headerState: CollapsingHeaderState,
    onBackPressed: () -> Unit
) {
    FilledTonalIconButton(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .offset {
                IntOffset(
                    x = 8.dp.roundToPx(),
                    y = 4.dp.roundToPx() - headerState.translation.roundToInt(),
                )
            },
        onClick = onBackPressed,
    ) {
        Image(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(Res.string.back),
        )
    }
}