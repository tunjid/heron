package com.tunjid.heron.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileRelationship
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileWithRelationship(
    modifier: Modifier,
    sharedElementScope: SharedElementScope,
    profileWithRelationship: ProfileWithRelationship,
    signedInProfileId: Id?,
    onProfileClicked: (Profile) -> Unit,
) = with(sharedElementScope) {
    AttributionLayout(
        modifier = modifier,
        avatar = {
            val profile = profileWithRelationship.profile
            AsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedPolygonShape.Circle)
                    .sharedElement(
                        key = profileWithRelationship.sharedElementKey()
                    )
                    .clickable { onProfileClicked(profile) },
                args = remember(profile.avatar) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile.displayName ?: profile.handle.id,
                        shape = RoundedPolygonShape.Circle,
                    )
                }
            )
        },
        label = {
            Column {
                ProfileName(
                    modifier = Modifier,
                    profile = profileWithRelationship.profile,
                    ellipsize = false,
                )
                Spacer(Modifier.height(4.dp))
                ProfileHandle(
                    modifier = Modifier,
                    profile = profileWithRelationship.profile,
                )
            }
        },
        action = {
            val isSignedInProfile = signedInProfileId == profileWithRelationship.profile.did
            AnimatedVisibility(
                visible = profileWithRelationship.relationship != null || isSignedInProfile,
                content = {
                    ProfileRelationship(
                        relationship = profileWithRelationship.relationship,
                        isSignedInProfile = isSignedInProfile,
                        onClick = {}
                    )
                },
            )
        },
    )
}

internal fun ProfileWithRelationship.sharedElementKey() =
    "profiles-${profile.did}"