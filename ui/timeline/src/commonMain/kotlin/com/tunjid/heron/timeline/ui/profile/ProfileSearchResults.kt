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

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
fun ProfileSearchResults(
    modifier: Modifier = Modifier,
    results: List<Profile>,
    onProfileClicked: (Profile) -> Unit,
) {
    LookaheadScope {
        ElevatedCard(
            modifier = modifier
                .animateBounds(this@LookaheadScope)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column {
                results.forEachIndexed { index, profile ->
                    key(profile.did.id) {
                        ProfileResultItem(
                            profile = profile,
                            onProfileClicked = onProfileClicked,
                            modifier = Modifier
                                .animateBounds(this@LookaheadScope)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }

                    if (index != results.lastIndex) {
                        key("${profile.did.id}-divider") {
                            HorizontalDivider(
                                modifier = Modifier
                                    .animateBounds(this@LookaheadScope)
                                    .padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.8.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileResultItem(
    profile: Profile,
    onProfileClicked: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    AttributionLayout(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProfileClicked(profile) },
        avatar = {
            AsyncImage(
                args = remember(profile.avatar) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                modifier = Modifier
                    .size(36.dp),
            )
        },
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ProfileName(profile = profile)
                ProfileHandle(profile = profile)
            }
        },
        action = { /* no follow/edit chip for mention autocomplete */ },
    )
}
