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

package com.tunjid.heron.timeline.ui.profile

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import com.tunjid.heron.data.core.models.Profile

@Composable
fun ProfileName(
    modifier: Modifier = Modifier,
    profile: Profile,
    ellipsize: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = remember(profile.displayName) {
            profile.displayName ?: ""
        },
        maxLines = if (ellipsize) 1 else Int.MAX_VALUE,
        style = LocalTextStyle.current.copy(fontWeight = Bold),
    )
}

@Composable
fun ProfileHandle(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Text(
        modifier = modifier,
        text = remember(profile.handle) {
            profile.handle.id
        },
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}
