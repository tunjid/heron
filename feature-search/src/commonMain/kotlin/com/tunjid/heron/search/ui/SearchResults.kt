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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileRelationship
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileSearchResult(
    panedSharedElementScope: PanedSharedElementScope,
    result: SearchResult.Profile,
    onProfileClicked: (SearchResult.Profile) -> Unit,
) = with(panedSharedElementScope) {
    AttributionLayout(
        modifier = Modifier
            .clickable { onProfileClicked(result) },
        avatar = {
            updatedMovableSharedElementOf(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onProfileClicked(result) },
                key = result.avatarSharedElementKey(),
                state = remember(result.profileWithRelationship.profile.avatar) {
                    ImageArgs(
                        url = result.profileWithRelationship.profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = result.profileWithRelationship.profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                }
            )
        },
        label = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProfileName(
                    profile = result.profileWithRelationship.profile
                )
                ProfileHandle(
                    profile = result.profileWithRelationship.profile
                )
            }
        },
        action = {
            ProfileRelationship(
                relationship = result.profileWithRelationship.relationship,
                isSignedInProfile = false,
                onClick = {}
            )
        }
    )
}

@Composable
internal fun PostSearchResult(
    panedSharedElementScope: PanedSharedElementScope,
    now: Instant,
    result: SearchResult.Post,
    onProfileClicked: (SearchResult.Post) -> Unit,
    onPostClicked: (SearchResult.Post) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier,
        onClick = {
            onPostClicked(result)
        },
        content = {
            Post(
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                panedSharedElementScope = panedSharedElementScope,
                now = now,
                post = result.post,
                embed = result.post.embed,
                isAnchoredInTimeline = false,
                avatarShape = RoundedPolygonShape.Circle,
                sharedElementPrefix = result.sharedElementPrefix(),
                createdAt = result.post.createdAt,
                onProfileClicked = { _, _ ->
                    onProfileClicked(result)
                },
                onPostClicked = {
                    onPostClicked(result)
                },
                onPostMediaClicked = { _, _, _ -> },
                onReplyToPost = {},
                onPostInteraction = onPostInteraction,
            )
        },
    )
}

internal fun SearchResult.Profile.avatarSharedElementKey(): String =
    "${sharedElementPrefix()}-${profileWithRelationship.profile.did.id}"

internal fun SearchResult.sharedElementPrefix() = when (this) {
    is SearchResult.Post.Top -> "top-post-search-result"
    is SearchResult.Post.Latest -> "latest-post-search-result"
    is SearchResult.Profile -> "profile-search-result"
}