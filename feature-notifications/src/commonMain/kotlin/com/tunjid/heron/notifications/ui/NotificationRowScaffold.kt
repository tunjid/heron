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

package com.tunjid.heron.notifications.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.treenav.compose.threepane.PaneMovableElementSharedTransitionScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotificationAggregateScaffold(
    paneMovableElementSharedTransitionScope: PaneMovableElementSharedTransitionScope<*>,
    modifier: Modifier = Modifier,
    isRead: Boolean,
    notification: Notification,
    profiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.width(UiTokens.avatarSize),
                contentAlignment = Alignment.TopCenter,
            ) {
                icon()
            }
            Spacer(Modifier.height(8.dp))
            if (!isRead) Badge(Modifier.size(4.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // TODO: Consider moving this to the VM.
            var isExpanded by rememberSaveable { mutableStateOf(false) }

            val renderedProfiles = remember(profiles) {
                when (profiles.size) {
                    in 0..6 -> profiles
                    else -> profiles.take(6)
                }
            }
            val expandButton = remember {
                movableContentWithReceiverOf<PaneMovableElementSharedTransitionScope<*>, Boolean> { expanded ->
                    ExpandButton(
                        isExpanded = expanded,
                        onExpansionToggled = { isExpanded = it }
                    )
                }
            }
            val items = remember {
                movableContentWithReceiverOf<
                        PaneMovableElementSharedTransitionScope<*>,
                        Boolean,
                        Notification,
                        List<Profile>,
                        >
                { isExpanded, notification, renderedProfiles ->
                    ExpandableProfiles(
                        isExpanded = isExpanded,
                        notification = notification,
                        renderedProfiles = renderedProfiles,
                        onProfileClicked = onProfileClicked,
                    )
                }
            }
            with(paneMovableElementSharedTransitionScope) {
                if (isExpanded) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (renderedProfiles.size > 1) expandButton(isExpanded)
                    items(isExpanded, notification, renderedProfiles)
                }
                else Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(isExpanded, notification, renderedProfiles)
                    if (renderedProfiles.size > 1) expandButton(isExpanded)
                }
            }
            Box(
                modifier = Modifier.animateBounds(
                    lookaheadScope = paneMovableElementSharedTransitionScope
                )
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PaneMovableElementSharedTransitionScope<*>.ExpandButton(
    isExpanded: Boolean,
    onExpansionToggled: (Boolean) -> Unit,
) {
    IconButton(
        modifier = Modifier
            .animateBounds(
                lookaheadScope = this@ExpandButton
            )
            .size(32.dp)
            .rotate(animateFloatAsState(if (isExpanded) 180f else 0f).value),
        onClick = {
            onExpansionToggled(!isExpanded)
        },
        content = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PaneMovableElementSharedTransitionScope<*>.ExpandableProfiles(
    isExpanded: Boolean,
    notification: Notification,
    renderedProfiles: List<Profile>,
    onProfileClicked: (Notification, Profile) -> Unit,
) {
    renderedProfiles.forEach { profile ->
        Row(
            modifier = Modifier.animateBounds(
                lookaheadScope = this@ExpandableProfiles,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(32.dp)
                    .paneSharedElement(
                        key = notification.avatarSharedElementKey(profile)
                    )
                    .clickable { onProfileClicked(notification, profile) },
                args = ImageArgs(
                    url = profile.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = profile.displayName ?: profile.handle.id,
                    shape = RoundedPolygonShape.Circle,
                )
            )
            AnimatedVisibility(
                visible = isExpanded,
                exit = fadeOut(),
            ) {
                Text(
                    modifier = Modifier
                        // Fill max width is needed so the text measuring doesn't cause
                        // animation glitches. This is also why the link is used for clicking
                        // as opposed to the full text.
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    text = remember {
                        buildAnnotatedString {
                            val profileText = profile.displayName ?: ""
                            append(profileText)
                            addLink(
                                clickable = LinkAnnotation.Clickable(profileText) {
                                    onProfileClicked(notification, profile)
                                },
                                start = 0,
                                end = profileText.length,
                            )
                        }
                    },
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun notificationText(
    notification: Notification,
    aggregatedSize: Int,
    singularResource: StringResource,
    pluralResource: StringResource,
): String {
    val author = notification.author
    val profileText = author.displayName ?: "@${author.handle}"
    return if (aggregatedSize <= 1) stringResource(singularResource, profileText)
    else stringResource(pluralResource, profileText, aggregatedSize)
}

internal fun Notification.avatarSharedElementKey(
    profile: Profile,
): String = "notification-${cid.id}-${profile.did.id}"