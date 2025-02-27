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

package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.timeline.utilities.displayName
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TimelineTitle(
    timeline: Timeline?,
    creator: Profile?,
    hasUpdates: Boolean,
    onPresentationSelected: (Timeline, Timeline.Presentation) -> Unit,
) {
    if (timeline != null) Row(
        modifier = Modifier
            .padding(
                horizontal = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeline.avatarImageArgs?.let { args ->
            AsyncImage(
                modifier = Modifier
                    .size(44.dp),
                args = args,
            )
            Spacer(Modifier.width(12.dp))
        }
        Box {
            Column {
                Text(
                    text = timeline.displayName(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    text = timeline.getDescription(creator),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (hasUpdates) Badge(
                modifier = Modifier.size(4.dp)
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
            }
        )
    }
}

private val Timeline.avatarImageArgs: ImageArgs?
    get() = when (this) {
        is Timeline.Home.Feed ->
            if (feedGenerator.avatar == null) null
            else ImageArgs(
                url = feedGenerator.avatar?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                shape = TimelineAvatarShape,
            )

        is Timeline.Home.List ->
            if (feedList.avatar == null) null
            else ImageArgs(
                url = feedList.avatar?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                shape = TimelineAvatarShape,
            )

        is Timeline.Home.Following,
        is Timeline.Profile,
            -> null
    }

private fun Timeline.getDescription(
    creator: Profile?,
): String = when (this) {
    is Timeline.Home.Feed -> creator?.displayName ?: feedGenerator.creatorId.id
    is Timeline.Home.List -> creator?.displayName ?: feedList.creatorId.id

    is Timeline.Home.Following,
    is Timeline.Profile,
        -> null
} ?: ""

private val TimelineAvatarShape = RoundedPolygonShape.Star(
    cornerSizeAtIndex = (0..<40).map { 40.dp },
    roundingRadius = 0.32f,
)