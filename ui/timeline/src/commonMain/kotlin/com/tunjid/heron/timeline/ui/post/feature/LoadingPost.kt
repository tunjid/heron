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

package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.ShimmerState
import com.tunjid.heron.ui.modifiers.ShimmerState.Companion.rememberShimmerState
import com.tunjid.heron.ui.modifiers.shimmer
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
internal fun LoadingPost(
    modifier: Modifier = Modifier,
    presentation: Timeline.Presentation,
) {
    val shimmerState = rememberShimmerState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = presentation.postVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(presentation.postContentSpacing),
    ) {
        presentation.placeholders.forEach { placeholder ->
            key(placeholder) {
                when (placeholder) {
                    Placeholder.Attribution -> AttributionPlaceholder(
                        shimmerState = shimmerState,
                        modifier = Modifier.padding(
                            start = 8.dp,
                            end = 8.dp,
                        ),
                    )

                    Placeholder.Text -> TextPlaceholders(
                        shimmerState = shimmerState,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 16.dp,
                        ),
                    )

                    Placeholder.Media -> MediaPlaceholder(
                        shimmerState = shimmerState,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributionPlaceholder(
    shimmerState: ShimmerState,
    modifier: Modifier = Modifier,
) {
    AttributionLayout(
        modifier = modifier,
        avatar = {
            Box(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clip(RoundedPolygonShape.Circle)
                    .shimmer(shimmerState),
            )
        },
        label = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer(shimmerState),
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer(shimmerState),
                )
            }
        },
    )
}

@Composable
private fun TextPlaceholders(
    shimmerState: ShimmerState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer(shimmerState),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer(shimmerState),
        )
    }
}

@Composable
private fun MediaPlaceholder(
    shimmerState: ShimmerState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shimmer(shimmerState),
    )
}

private enum class Placeholder {
    Attribution,
    Text,
    Media,
}

private val Timeline.Presentation.placeholders: List<Placeholder>
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> listOf(
            Placeholder.Attribution,
            Placeholder.Text,
        )

        Timeline.Presentation.Media.Expanded -> listOf(
            Placeholder.Attribution,
            Placeholder.Media,
        )

        Timeline.Presentation.Media.Condensed,
        Timeline.Presentation.Media.Grid,
        -> listOf(
            Placeholder.Media,
        )
    }

private val Timeline.Presentation.postVerticalPadding: Dp
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

private val Timeline.Presentation.postContentSpacing: Dp
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }
