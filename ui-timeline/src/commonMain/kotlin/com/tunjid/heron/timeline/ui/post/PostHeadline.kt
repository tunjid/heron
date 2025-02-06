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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.TimeDelta
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostHeadline(
    now: Instant,
    createdAt: Instant,
    author: Profile,
    postId: Id,
    sharedElementPrefix: String,
    panedSharedElementScope: PanedSharedElementScope,
) = with(panedSharedElementScope) {
    Column {
        val primaryText = author.displayName ?: author.handle.id
        val secondaryText = author.handle.id.takeUnless { it == primaryText }

        Row(horizontalArrangement = spacedBy(4.dp)) {
            ProfileName(
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        key = author.textSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            text = primaryText
                        ),
                    ),
                profile = author,
            )

            TimeDelta(
                modifier = Modifier.alignByBaseline(),
                delta = now - createdAt,
            )
        }
        if (secondaryText != null) {
            Spacer(Modifier.height(2.dp))
            ProfileHandle(
                modifier = Modifier
                    .sharedElement(
                        key = author.textSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            text = secondaryText
                        ),
                    ),
                profile = author,
            )

        }
    }
}

private fun Profile.textSharedElementKey(
    prefix: String,
    postId: Id,
    text: String,
): String = "$prefix-${postId.id}-${did.id}-$text"