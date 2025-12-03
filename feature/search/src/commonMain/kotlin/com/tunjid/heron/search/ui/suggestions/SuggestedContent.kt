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

package com.tunjid.heron.search.ui.suggestions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.JoinFull
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.search.ui.searchresults.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.timeline.utilities.roundComponent
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.discover_feeds
import heron.feature.search.generated.resources.hot
import heron.feature.search.generated.resources.post_count
import heron.feature.search.generated.resources.starter_packs
import heron.feature.search.generated.resources.suggested_accounts
import heron.feature.search.generated.resources.trend_started
import heron.feature.search.generated.resources.trending_title
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SuggestedContent(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    trends: List<Trend>,
    suggestedProfiles: List<ProfileWithViewerState>,
    starterPacksWithMembers: List<SuggestedStarterPack>,
    feedGenerators: List<FeedGenerator>,
    timelineRecordUrisToPinnedStatus: Map<RecordUri?, Boolean>,
    onTrendClicked: (Trend) -> Unit,
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
    onListMemberClicked: (ListMember) -> Unit,
    onFeedGeneratorClicked: (FeedGenerator, String) -> Unit,
    onUpdateTimelineClicked: (Timeline.Update) -> Unit,
) {
    val now = remember { Clock.System.now() }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
        ),
    ) {
        item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .animateItem(),
                icon = Icons.AutoMirrored.Rounded.ShowChart,
                title = stringResource(Res.string.trending_title),
            )
        }
        itemsIndexed(
            items = trends.take(5),
            key = { _, trend -> trend.link },
            itemContent = { index, trend ->
                Trend(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .clickable { onTrendClicked(trend) }
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    index = index,
                    now = now,
                    trend = trend,
                    onTrendClicked = onTrendClicked,
                )
            },
        )
        if (suggestedProfiles.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.AccountCircle,
                title = stringResource(Res.string.suggested_accounts),
            )
        }
        items(
            items = suggestedProfiles.take(5),
            key = { suggestedProfile -> suggestedProfile.profile.did.id },
            itemContent = { profileWithViewerState ->
                ProfileWithViewerState(
                    modifier = Modifier
                        .clickable {
                            onProfileClicked(
                                profileWithViewerState.profile,
                                SuggestedProfilesSharedElementPrefix,
                            )
                        }
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    signedInProfileId = null,
                    profile = profileWithViewerState.profile,
                    viewerState = profileWithViewerState.viewerState,
                    profileSharedElementKey = { profile ->
                        profile.avatarSharedElementKey(SuggestedProfilesSharedElementPrefix)
                    },
                    onProfileClicked = { profile ->
                        onProfileClicked(
                            profile,
                            SuggestedProfilesSharedElementPrefix,
                        )
                    },
                    onViewerStateClicked = { onViewerStateClicked(profileWithViewerState) },
                )
            },
        )
        if (starterPacksWithMembers.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.JoinFull,
                title = stringResource(Res.string.starter_packs),
            )
        }
        items(
            items = starterPacksWithMembers.take(5),
            key = { starterPackWithMember -> starterPackWithMember.starterPack.cid.id },
            itemContent = { starterPackWithMember ->
                SuggestedStarterPack(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    starterPackWithMembers = starterPackWithMember,
                    onListMemberClicked = onListMemberClicked,
                )
            },
        )
        if (feedGenerators.isNotEmpty()) item {
            TrendTitle(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
                icon = Icons.Rounded.RssFeed,
                title = stringResource(Res.string.discover_feeds),
            )
        }
        items(
            items = feedGenerators.take(5),
            key = { feedGenerator -> feedGenerator.cid.id },
            itemContent = { feedGenerator ->
                FeedGenerator(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(
                            vertical = 4.dp,
                            horizontal = 16.dp,
                        )
                        .clickable {
                            onFeedGeneratorClicked(
                                feedGenerator,
                                SuggestedFeedsSharedElementPrefix,
                            )
                        }
                        .animateItem(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    sharedElementPrefix = SuggestedFeedsSharedElementPrefix,
                    feedGenerator = feedGenerator,
                    status = when (timelineRecordUrisToPinnedStatus[feedGenerator.uri]) {
                        true -> Timeline.Home.Status.Pinned
                        false -> Timeline.Home.Status.Saved
                        null -> Timeline.Home.Status.None
                    },
                    onFeedGeneratorStatusUpdated = onUpdateTimelineClicked,
                )
            },
        )
        item {
            Spacer(
                Modifier
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(UiTokens.bottomNavHeight),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendTitle(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
) {
    Column(
        modifier = modifier.padding(
            vertical = 8.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun Trend(
    modifier: Modifier = Modifier,
    index: Int,
    now: Instant,
    trend: Trend,
    onTrendClicked: (Trend) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = trendTitle(index, trend),
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier
                        .width(8.dp),
                )
                TrendAvatars(
                    trend = trend,
                )
                Text(
                    text = trendDetails(trend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(
            modifier = Modifier
                .weight(1f),
        )
        FilterChip(
            selected = false,
            shape = CircleShape,
            onClick = { onTrendClicked(trend) },
            leadingIcon = {
                when (trend.status) {
                    Trend.Status.Hot -> Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = "",
                    )

                    null -> Unit
                }
            },
            label = {
                Text(
                    text = when (trend.status) {
                        Trend.Status.Hot -> stringResource(Res.string.hot)
                        null -> stringResource(
                            Res.string.trend_started,
                            remember(
                                now,
                                trend.startedAt,
                            ) { now - trend.startedAt }.roundComponent(),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}

@Composable
private fun TrendAvatars(trend: Trend) {
    trend.actors
        .take(MaxTrendAvatars)
        .forEachIndexed { profileIndex, profile ->
            AsyncImage(
                modifier = Modifier
                    .zIndex((MaxTrendAvatars - profileIndex).toFloat())
                    .size(20.dp)
                    .offset {
                        IntOffset(x = (-8 * profileIndex).dp.roundToPx(), y = 0)
                    },
                args = ImageArgs(
                    url = profile.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    shape = RoundedPolygonShape.Circle,
                ),
            )
        }
}

private fun trendTitle(index: Int, trend: Trend) =
    "${index + 1}. ${trend.displayName ?: ""}"

@Composable
private fun trendDetails(trend: Trend): String {
    val postCount = stringResource(Res.string.post_count, format(trend.postCount))
    return when (val category = trend.category) {
        null -> postCount
        else -> "$postCount · $category"
    }
}

private const val SuggestedProfilesSharedElementPrefix = "suggested-profile"
private const val SuggestedFeedsSharedElementPrefix = "suggested-feeds"

private const val MaxTrendAvatars = 3
