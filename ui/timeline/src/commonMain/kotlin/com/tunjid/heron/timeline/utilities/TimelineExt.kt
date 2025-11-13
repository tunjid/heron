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

package com.tunjid.heron.timeline.utilities

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.subtitleSharedElementKey
import com.tunjid.heron.ui.titleSharedElementKey
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.feed_by
import heron.ui.timeline.generated.resources.likes
import heron.ui.timeline.generated.resources.list_by
import heron.ui.timeline.generated.resources.media
import heron.ui.timeline.generated.resources.posts
import heron.ui.timeline.generated.resources.replies
import heron.ui.timeline.generated.resources.starter_pack_by
import heron.ui.timeline.generated.resources.videos
import org.jetbrains.compose.resources.stringResource

@Composable
fun Timeline.displayName() = when (this) {
    is Timeline.Home.Feed -> name
    is Timeline.Home.Following -> name
    is Timeline.Home.List -> name
    is Timeline.StarterPack -> starterPack.name
    is Timeline.Profile -> when (type) {
        Timeline.Profile.Type.Media -> stringResource(Res.string.media)
        Timeline.Profile.Type.Posts -> stringResource(Res.string.posts)
        Timeline.Profile.Type.Likes -> stringResource(Res.string.likes)
        Timeline.Profile.Type.Replies -> stringResource(Res.string.replies)
        Timeline.Profile.Type.Videos -> stringResource(Res.string.videos)
    }.capitalize(Locale.current)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TimelineTitle(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    timeline: Timeline?,
    sharedElementPrefix: String?,
    hasUpdates: Boolean,
    onPresentationSelected: (Timeline, Timeline.Presentation) -> Unit,
) = with(movableElementSharedTransitionScope) {
    if (timeline != null) Row(
        modifier = modifier
            .padding(
                horizontal = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatar = timeline.avatar
        AsyncImage(
            modifier = Modifier
                .paneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = timeline.avatarSharedElementKey(sharedElementPrefix),
                    ),
                    zIndexInOverlay = UiTokens.appBarSharedElementOverlayZIndex,
                )
                .size(44.dp),
            args = remember(avatar) {
                ImageArgs(
                    url = avatar.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    shape = timeline.shape,
                )
            },
        )

        Spacer(Modifier.width(12.dp))

        Box {
            Column {
                Text(
                    modifier = Modifier
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = timeline.titleSharedElementKey(sharedElementPrefix),
                            ),
                            zIndexInOverlay = UiTokens.appBarSharedElementOverlayZIndex,
                        ),
                    text = timeline.displayName(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    modifier = Modifier
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = timeline.subtitleSharedElementKey(sharedElementPrefix),
                            ),
                            zIndexInOverlay = UiTokens.appBarSharedElementOverlayZIndex,
                        ),
                    text = timeline.creator(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (hasUpdates) Badge(
                modifier = Modifier.size(4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        TimelinePresentationSelector(
            selected = timeline.presentation,
            available = timeline.supportedPresentations,
            onPresentationSelected = { presentation ->
                onPresentationSelected(
                    timeline,
                    presentation,
                )
            },
        )
    }
}

val Timeline.description: String
    get() = when (this) {
        is Timeline.Home.Feed -> feedGenerator.description
        is Timeline.Home.List -> feedList.description
        is Timeline.StarterPack -> starterPack.description

        is Timeline.Home.Following,
        is Timeline.Profile,
        -> null
    } ?: ""

@Composable
private fun Timeline.creator(): String = when (this) {
    is Timeline.Home.Feed -> stringResource(
        Res.string.feed_by,
        feedGenerator.creator.handle.id,
    )

    is Timeline.Home.List -> stringResource(
        Res.string.list_by,
        feedList.creator.handle.id,
    )

    is Timeline.StarterPack -> stringResource(
        Res.string.starter_pack_by,
        starterPack.creator.handle.id,
    )

    is Timeline.Home.Following,
    is Timeline.Profile,
    -> null
} ?: ""

val Timeline.sharedElementPrefix get() = sourceId

fun Timeline.sharedElementPrefix(
    quotingPostUri: PostUri?,
) = sourceId.withQuotingPostUriPrefix(
    quotingPostUri = quotingPostUri,
)

fun LazyStaggeredGridState.pendingOffsetFor(
    item: TimelineItem,
) = layoutInfo
    .visibleItemsInfo
    .first { it.key == item.id }
    .offset
    .y
    .toFloat()

val TimelineItem.canAutoPlayVideo: Boolean
    get() = appliedLabels.postLabelVisibilitiesToDefinitions.canAutoPlayVideo

private val Timeline.avatar: ImageUri
    get() = when (this) {
        is Timeline.Home.Feed -> feedGenerator.avatar
        is Timeline.Home.List -> feedList.avatar
        is Timeline.StarterPack -> starterPack.list?.avatar
        is Timeline.Home.Following,
        is Timeline.Profile,
        -> BlueskyClouds
    } ?: BlueskyClouds

private fun Timeline.avatarSharedElementKey(
    sharedElementPrefix: String?,
): String = when (this) {
    is Timeline.Home.Feed -> feedGenerator.avatarSharedElementKey(sharedElementPrefix)
    is Timeline.Home.List -> feedList.avatarSharedElementKey(sharedElementPrefix)
    is Timeline.StarterPack -> starterPack.avatarSharedElementKey(sharedElementPrefix)
    is Timeline.Home.Following -> "$sharedElementPrefix-following"
    is Timeline.Profile -> "$sharedElementPrefix-${profileId.id}"
}

private fun Timeline.titleSharedElementKey(
    sharedElementPrefix: String?,
): String = when (this) {
    is Timeline.Home.Feed -> titleSharedElementKey(
        prefix = sharedElementPrefix,
        type = feedGenerator.uri,
    )

    is Timeline.Home.List -> titleSharedElementKey(
        prefix = sharedElementPrefix,
        type = feedList.uri,
    )

    is Timeline.StarterPack -> titleSharedElementKey(
        prefix = sharedElementPrefix,
        type = starterPack.uri,
    )

    is Timeline.Home.Following -> "$sharedElementPrefix-following-title"
    is Timeline.Profile -> "$sharedElementPrefix-${profileId.id}-title"
}

private fun Timeline.subtitleSharedElementKey(
    sharedElementPrefix: String?,
): String = when (this) {
    is Timeline.Home.Feed -> subtitleSharedElementKey(
        prefix = sharedElementPrefix,
        type = feedGenerator.uri,
    )

    is Timeline.Home.List -> subtitleSharedElementKey(
        prefix = sharedElementPrefix,
        type = feedList.uri,
    )

    is Timeline.StarterPack -> subtitleSharedElementKey(
        prefix = sharedElementPrefix,
        type = starterPack.uri,
    )

    is Timeline.Home.Following -> "$sharedElementPrefix-following-subtitle"
    is Timeline.Profile -> "$sharedElementPrefix-${profileId.id}-subtitle"
}

private val Timeline.shape: RoundedPolygonShape
    get() = when (this) {
        is Timeline.Home.Feed -> FeedGeneratorCollectionShape
        is Timeline.Home.List -> ListCollectionShape
        is Timeline.StarterPack -> StarterPackCollectionShape
        is Timeline.Home.Following,
        is Timeline.Profile,
        -> RoundedPolygonShape.Circle
    }
