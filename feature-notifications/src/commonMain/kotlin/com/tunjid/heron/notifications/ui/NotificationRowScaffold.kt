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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotificationAggregateScaffold(
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
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
        Box(
            modifier = Modifier.width(UiTokens.avatarSize),
            contentAlignment = Alignment.TopCenter,
        ) {
            icon()
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (profiles.size) {
                    in 0..6 -> profiles
                    else -> profiles.take(6)
                }.forEach { profile ->
                    panedSharedElementScope.updatedMovableSharedElementOf(
                        key = notification.avatarSharedElementKey(profile),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onProfileClicked(notification, profile) },
                        state = remember {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = profile.displayName ?: profile.handle.id,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
                        sharedElement = { state, innerModifier ->
                            AsyncImage(state, innerModifier)
                        }
                    )
                }
            }
            Box {
                content()
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
    return if (aggregatedSize == 0) stringResource(singularResource, profileText)
    else stringResource(pluralResource, profileText, aggregatedSize)
}

internal fun Notification.avatarSharedElementKey(
    profile: Profile,
): String = "notification-${cid.id}-${profile.did.id}"