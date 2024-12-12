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

import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.feed.ui.FeedItem
import com.tunjid.heron.feed.utilities.format
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

@Composable
internal fun ProfileScreen(
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
            initialExpandedHeight = with(density) { 400.dp.toPx() },
            decayAnimationSpec = splineBasedDecay(density)
        )
    }

    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.feed)

    CollapsingHeaderLayout(
        state = headerState,
        headerContent = {
            ProfileHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -headerState.translation.roundToInt()
                        )
                    },
                profile = state.profile
            )
        },
        body = {
            LazyVerticalStaggeredGrid(
                modifier = modifier
                    .fillMaxSize(),
                state = gridState,
                columns = StaggeredGridCells.Adaptive(400.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = items,
                    key = FeedItem::id,
                    itemContent = { item ->
                        FeedItem(
                            modifier = Modifier
                                .fillMaxWidth(),
                            now = remember { Clock.System.now() },
                            item = item,
                            onPostClicked = {},
                            onProfileClicked = {},
                            onImageClicked = {},
                            onReplyToPost = {},
                        )
                    }
                )
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
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
        ) {
            ProfileHeadline(
                modifier = Modifier.fillMaxWidth(),
                profile = profile,
            )
            ProfileStats(
                modifier = Modifier.fillMaxWidth(),
                profile = profile,
            )
            Text(text = profile.description ?: "")
        }
    }
}

@Composable
private fun ProfileHeadline(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Row(modifier) {
        Column {
            val primaryText = profile.displayName ?: profile.handle.id
            val secondaryText = profile.handle.id.takeUnless { it == primaryText }

            Text(
                modifier = Modifier.weight(1f),
                text = primaryText,
                maxLines = 1,
                style = LocalTextStyle.current.copy(fontWeight = Bold),
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
    }
}

@Composable
private fun ProfileStats(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Statistic(
            value = profile.followersCount ?: 0,
            description = "Followers",
            onClick = {}
        )
        Statistic(
            value = profile.followsCount ?: 0,
            description = "Following",
            onClick = {}
        )
        Statistic(
            value = profile.postsCount ?: 0,
            description = "Posts",
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
        horizontalAlignment = Alignment.CenterHorizontally,
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