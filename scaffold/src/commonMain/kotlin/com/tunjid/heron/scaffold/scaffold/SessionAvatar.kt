package com.tunjid.heron.scaffold.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.profile.AvatarLiveZIndex
import com.tunjid.heron.profile.AvatarZIndex
import com.tunjid.heron.profile.ProfileLiveChip
import com.tunjid.heron.profile.profileLiveAvatarBorder
import com.tunjid.heron.profile.withProfileAvatarLiveSharedElementPrefix
import com.tunjid.heron.scaffold.identity.IdentityState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.modifiers.shapedCombinedClickable
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PaneScaffoldState.SessionAvatar(
    modifier: Modifier = Modifier,
    status: IdentityState.SwitchStatus,
    isLive: Boolean,
    profileAvatar: ImageUri?,
    profileDescription: String?,
    profileId: ProfileId,
    signedInProfileId: ProfileId?,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val isSignedInProfile = profileId == signedInProfileId
    Box(
        modifier = modifier,
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            sharedContentState = rememberSharedContentState(
                key = when {
                    isSignedInProfile -> UiTokens.SignedInUserAvatarSharedElementKey
                    else -> profileId.id
                },
            ),
            zIndexInOverlay = AvatarZIndex,
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap()
                    .ifTrue(
                        predicate = isLive,
                        block = Modifier::profileLiveAvatarBorder,
                    )
                    .shapedCombinedClickable(
                        CircleShape,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    ),
                args = remember(profileAvatar) {
                    ImageArgs(
                        url = profileAvatar?.uri,
                        contentDescription = profileDescription,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
            if (isLive && isSignedInProfile) PaneStickySharedElement(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                sharedContentState = rememberSharedContentState(
                    key = UiTokens.SignedInUserAvatarSharedElementKey
                        .withProfileAvatarLiveSharedElementPrefix(),
                ),
                zIndexInOverlay = AvatarLiveZIndex,
            ) {
                ProfileLiveChip()
            }
            if (status is IdentityState.SwitchStatus.Switching && status.session.profileId == profileId) CircularWavyProgressIndicator(
                progress = { 1f },
                trackColor = MaterialTheme.colorScheme.surface,
                amplitude = { 1f },
                modifier = Modifier
                    .size(48.dp),
            )
        }
    }
}
