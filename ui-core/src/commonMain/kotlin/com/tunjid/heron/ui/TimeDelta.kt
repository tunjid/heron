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

package com.tunjid.heron.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Duration

@Composable
fun TimeDelta(
    delta: Duration,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = delta.toComponents { days, hours, minutes, seconds, _ ->
            when {
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                seconds > 0 -> "${seconds}s"
                seconds < 0 || minutes < 0 || hours < 0 || days < 0 -> "The Future"
                else -> "Now"
            }
        },
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}
