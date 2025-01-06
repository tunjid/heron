package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.shapes.ImageShape
import com.tunjid.heron.ui.SharedElementScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootDestinationTopAppBar(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    signedInProfile: Profile?,
    title: @Composable () -> Unit = {},
    onSignedInProfileClicked: (Profile, String) -> Unit,
) = with(sharedElementScope) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            Image(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(36.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(AppLogo),
                        animatedVisibilityScope = sharedElementScope,
                        boundsTransform = { _, _ ->
                            spring(stiffness = Spring.StiffnessLow)
                        }
                    ),
                imageVector = AppLogo,
                contentDescription = null,
            )
        },
        title = title,
        actions = {
            AnimatedVisibility(
                visible = signedInProfile != null
            ) {
                signedInProfile?.let { profile ->
                    AsyncImage(
                        modifier = Modifier
                            .size(24.dp)
                            .sharedElement(
                                key = SignedInUserAvatarSharedElementKey,
                            )
                            .clickable {
                                onSignedInProfileClicked(
                                    profile,
                                    SignedInUserAvatarSharedElementKey
                                )
                            },
                        args = remember(profile) {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentDescription = signedInProfile.displayName,
                                contentScale = ContentScale.Crop,
                                shape = ImageShape.Circle,
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        },
    )
}

private const val SignedInUserAvatarSharedElementKey = "self"
