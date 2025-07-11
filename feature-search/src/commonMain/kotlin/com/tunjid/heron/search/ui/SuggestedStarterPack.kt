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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.OverlappingAvatarRow
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.by_creator
import org.jetbrains.compose.resources.stringResource

data class SuggestedStarterPack(
    val starterPack: StarterPack,
    val members: List<ListMember>,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SuggestedStarterPack(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    starterPackWithMembers: SuggestedStarterPack,
    onListMemberClicked: (ListMember) -> Unit,
) = with(movableElementSharedTransitionScope) {
    OutlinedCard(
        modifier = modifier,
        content = {
            OverlappingAvatarRow(
                overlap = AvatarOverlap,
                maxItems = MaxAvatars,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                val count = MaxAvatars - 1
                if (starterPackWithMembers.members.isEmpty()) (0..<count).forEach { index ->
                    Surface(
                        modifier = Modifier
                            .zIndex((MaxAvatars - index).toFloat())
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = CircleShape
                    ) { }
                }
                else starterPackWithMembers.members.take(count)
                    .forEachIndexed { index, listMember ->
                        updatedMovableStickySharedElementOf(
                            modifier = Modifier
                                .zIndex((MaxAvatars - index).toFloat())
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { onListMemberClicked(listMember) },
                            sharedContentState = with(movableElementSharedTransitionScope) {
                                rememberSharedContentState(
                                    key = listMember.avatarSharedElementKey(),
                                )
                            },
                            state = remember(listMember.subject.avatar) {
                                ImageArgs(
                                    url = listMember.subject.avatar?.uri,
                                    contentScale = ContentScale.Crop,
                                    shape = RoundedPolygonShape.Circle,
                                )
                            },
                            sharedElement = { state, modifier ->
                                AsyncImage(state, modifier)
                            }
                        )
                    }
                starterPackWithMembers.starterPack.list?.listItemCount?.let { joined ->
                    val itemsLeft = joined - starterPackWithMembers.members.size
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(56.dp),
                                shape = CircleShape,
                            )
                            .zIndex(0f)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = AvatarOverlap)
                                .align(Alignment.Center),
                            text = profilesLeftInStarterPack(itemsLeft),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = starterPackWithMembers.starterPack.name
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = stringResource(
                    Res.string.by_creator,
                    remember(starterPackWithMembers.starterPack.creator.handle) {
                        starterPackWithMembers.starterPack.creator.handle.id
                    }
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(
                modifier = Modifier
                    .height(16.dp),
            )
        }
    )
}

internal fun ListMember.avatarSharedElementKey(): String =
    "suggested-list-member-${subject.did.id}"

private fun profilesLeftInStarterPack(itemsLeft: Long) = "+${format(itemsLeft)}"

private val AvatarOverlap = 16.dp

private const val MaxAvatars = 10