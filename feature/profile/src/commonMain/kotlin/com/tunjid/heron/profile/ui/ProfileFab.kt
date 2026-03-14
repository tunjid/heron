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

package com.tunjid.heron.profile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.icons.AtmosphereIcons
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.mention
import heron.feature.profile.generated.resources.post
import heron.feature.profile.generated.resources.publish_with_leaflet
import heron.feature.profile.generated.resources.publish_with_pckt
import heron.feature.profile.generated.resources.write_something
import heron.ui.core.generated.resources.feed_generator_create
import heron.ui.core.generated.resources.sign_in
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class ProfileFabState(
    val text: StringResource,
    val icon: ImageVector,
) {

    data object SignedOut : ProfileFabState(
        text = CommonStrings.sign_in,
        icon = Icons.AutoMirrored.Rounded.Login,
    )

    sealed class SignedIn(
        text: StringResource,
        icon: ImageVector,
    ) : ProfileFabState(text, icon) {

        data object Feed : SignedIn(
            text = CommonStrings.feed_generator_create,
            icon = Icons.Rounded.Add,
        )

        data object Edit : SignedIn(
            text = Res.string.post,
            icon = Icons.Rounded.Edit,
        )

        data object Mention : SignedIn(
            text = Res.string.mention,
            icon = Icons.Rounded.AlternateEmail,
        )

        data object Writing : SignedIn(
            text = Res.string.write_something,
            icon = Icons.Rounded.Edit,
        )
    }
}

private data object WritingsFabSharedElementKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.ProfileFab(
    modifier: Modifier = Modifier,
    state: ProfileFabState,
    fabExpanded: Boolean,
    onStateClicked: (ProfileFabState) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    AnimatedContent(
        modifier = modifier,
        targetState = isExpanded,
    ) { showOptions ->
        if (showOptions) Surface(
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = WritingsFabSharedElementKey,
                    ),
                    animatedVisibilityScope = this@AnimatedContent,
                ),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            content = {
                val uriHandler = LocalUriHandler.current
                SameWidthColumn(
                    modifier = Modifier
                        .wrapContentWidth(),
                ) {
                    WritingsOptionItem(
                        modifier = Modifier,
                        icon = AtmosphereIcons.Leaflet,
                        text = stringResource(Res.string.publish_with_leaflet),
                        onClick = {
                            isExpanded = false
                            uriHandler.openUri(LeafletPage)
                        },
                    )
                    WritingsOptionItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = AtmosphereIcons.Pckt,
                        text = stringResource(Res.string.publish_with_pckt),
                        onClick = {
                            isExpanded = false
                            uriHandler.openUri(PcktPage)
                        },
                    )
                }
            },
        )
        else PaneFab(
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = WritingsFabSharedElementKey,
                    ),
                    animatedVisibilityScope = this@AnimatedContent,
                ),
            text = stringResource(state.text),
            icon = state.icon,
            expanded = fabExpanded,
            onClick = {
                when (state) {
                    ProfileFabState.SignedOut,
                    ProfileFabState.SignedIn.Feed,
                    ProfileFabState.SignedIn.Edit,
                    ProfileFabState.SignedIn.Mention,
                    -> onStateClicked(state)
                    ProfileFabState.SignedIn.Writing -> isExpanded = true
                }
            },
        )
    }
}

@Composable
private fun WritingsOptionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp,
            )
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}

@Composable
fun SameWidthColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        layout(
            width = placeables.first().width,
            height = placeables.sumOf(Placeable::height),
        ) {
            var yPosition = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = 0, y = yPosition)
                yPosition += placeable.height
            }
        }
    }
}

private const val LeafletPage = "https://leaflet.pub/home"
private const val PcktPage = "https://pckt.blog/atproto/identify"
