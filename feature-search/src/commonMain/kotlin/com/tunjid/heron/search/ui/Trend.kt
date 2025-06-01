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

package com.tunjid.heron.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.timeline.utilities.roundComponent
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.post_count
import heron.feature_search.generated.resources.trend_started
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Trend(
    modifier: Modifier = Modifier,
    index: Int,
    now: Instant,
    trend: Trend,
    onTrendClicked: (Trend) -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onTrendClicked(trend) }
            .padding(
                vertical = 4.dp,
                horizontal = 24.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = trendTitle(index, trend)
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .width(8.dp)
                )
                TrendAvatars(
                    trend
                )
                Text(
                    text = trendDetails(trend),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(
            modifier = Modifier
                .weight(1f)
        )
        FilterChip(
            selected = false,
            shape = CircleShape,
            onClick = { onTrendClicked(trend) },
            label = {
                Text(
                    text = stringResource(
                        Res.string.trend_started,
                        remember(now, trend.startedAt) { now - trend.startedAt }.roundComponent(),
                    ),
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
                )
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

private const val MaxTrendAvatars = 3