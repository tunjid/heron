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

package com.tunjid.heron.timeline.ui.standard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.NotificationsActive
import com.tunjid.heron.ui.icons.NotificationsOff
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.standard_publication_subscribe_to
import heron.ui.core.generated.resources.standard_publication_unsubscribe_from
import org.jetbrains.compose.resources.stringResource

@Composable
fun PublicationSubscriptionIcon(
    modifier: Modifier = Modifier,
    subscribed: Boolean,
    iconSize: Dp,
    iconTint: Color = LocalContentColor.current,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = subscribed,
        transitionSpec = {
            SubscriptionContentTransform
        },
    ) { isSubscribed ->
        Icon(
            modifier = Modifier
                .size(iconSize),
            imageVector =
            if (isSubscribed) HeronIcons.NotificationsOff
            else HeronIcons.NotificationsActive,
            contentDescription = stringResource(
                if (isSubscribed) CommonStrings.standard_publication_unsubscribe_from
                else CommonStrings.standard_publication_subscribe_to,
            ),
            tint = iconTint,
        )
    }
}

private val SubscriptionContentTransform = fadeIn() togetherWith fadeOut()
