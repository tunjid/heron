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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlin.time.Instant

@Composable
fun PostHeadline(
    now: Instant,
    createdAt: Instant,
    author: Profile,
    postId: PostId,
    sharedElementPrefix: String,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onPostClicked: () -> Unit,
    onAuthorClicked: () -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    Column {
        val primaryText = author.displayName ?: author.handle.id
        val secondaryText = author.handle.id.takeUnless { it == primaryText }

        Row(horizontalArrangement = spacedBy(4.dp)) {
            PaneStickySharedElement(
                sharedContentState = rememberSharedContentState(
                    key = author.textSharedElementKey(
                        prefix = sharedElementPrefix,
                        postId = postId,
                        text = primaryText,
                    ),
                ),
            ) {
                ProfileName(
                    modifier = Modifier
                        .clickable { onAuthorClicked() },
                    profile = author,
                )
            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPostClicked() },
            )
            TimeDelta(
                modifier = Modifier.alignByBaseline(),
                delta = now - createdAt,
            )
        }
        if (secondaryText != null) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = spacedBy(4.dp)) {
                PaneStickySharedElement(
                    modifier = Modifier,
                    sharedContentState = rememberSharedContentState(
                        key = author.textSharedElementKey(
                            prefix = sharedElementPrefix,
                            postId = postId,
                            text = secondaryText,
                        ),
                    ),
                ) {
                    ProfileHandle(
                        modifier = Modifier
                            .clickable { onAuthorClicked() },
                        profile = author,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPostClicked() },
                )
            }
        }
    }
}

private fun Profile.textSharedElementKey(
    prefix: String,
    postId: PostId,
    text: String,
): String = "$prefix-${postId.id}-${did.id}-$text"
