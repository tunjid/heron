package com.tunjid.heron.search.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.profile.ProfileHandle
import com.tunjid.heron.ui.profile.ProfileName
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileSearchResult(
    sharedElementScope: SharedElementScope,
    result: SearchResult.Profile,
    onProfileClicked: (SearchResult.Profile) -> Unit,
) = with(sharedElementScope) {
    AttributionLayout(
        modifier = Modifier
            .clickable { onProfileClicked(result) },
        avatar = {
            updatedMovableSharedElementOf(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onProfileClicked(result) },
                key = result.avatarSharedElementKey(),
                state = remember(result.profile.avatar) {
                    ImageArgs(
                        url = result.profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = result.profile.contentDescription,
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
                    profile = result.profile
                )
                ProfileHandle(
                    profile = result.profile
                )
            }
        },
    )
}

fun SearchResult.Profile.avatarSharedElementKey(): String =
    "search-result-${profile.did.id}"