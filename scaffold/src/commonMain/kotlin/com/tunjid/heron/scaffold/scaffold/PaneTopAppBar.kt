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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.threepane.ThreePane
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.go_back
import org.jetbrains.compose.resources.stringResource

data object PaneTopAppBar {

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun RootDestination(
        modifier: Modifier = Modifier,
        panedSharedElementScope: PanedSharedElementScope,
        signedInProfile: Profile?,
        title: @Composable () -> Unit = {},
        onSignedInProfileClicked: (Profile, String) -> Unit,
    ) = with(panedSharedElementScope) {
        TopAppBar(
            modifier = modifier,
            navigationIcon = {
                panedSharedElementScope.AppLogo(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(36.dp)
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
                                .size(32.dp)
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
                                    shape = RoundedPolygonShape.Circle,
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
            },
        )
    }

    @Composable
    fun PoppableDestination(
        modifier: Modifier = Modifier,
        panedSharedElementScope: PanedSharedElementScope,
        title: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
        onBackPressed: () -> Unit,
    ) {
        TopAppBar(
            modifier = modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            navigationIcon = {
                IconButton(
                    modifier = Modifier,
                    onClick = onBackPressed,
                ) {
                    Icon(
                        imageVector =
                        if (panedSharedElementScope.paneState.pane == ThreePane.Primary) Icons.AutoMirrored.Rounded.ArrowBack
                        else Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.go_back),
                    )
                }
            },
            title = title,
            actions = actions,
        )
    }
}


private data object SignedInUserAvatarSharedElementKey
