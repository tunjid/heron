package com.tunjid.heron.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileWithRelationship
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileRelationship
import com.tunjid.heron.ui.AttributionLayout

@Composable
fun ProfileWithRelationship(
    modifier: Modifier,
    profileWithRelationship: ProfileWithRelationship,
    signedInProfileId: Id?,
) {
    AttributionLayout(
        modifier = modifier,
        avatar = null,
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