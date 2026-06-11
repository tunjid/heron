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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.standard_publication_subscribe_to
import heron.ui.core.generated.resources.standard_publication_unsubscribe_from
import org.jetbrains.compose.resources.stringResource

@Composable
fun PublicationSubscriptionIcon(
    modifier: Modifier = Modifier,
    subscribed: Boolean,
    iconSize: Dp,
) {
    val swing = remember { Animatable(0f) }
    val initial = remember { mutableStateOf(true) }

    AnimatedContent(
        modifier = modifier.graphicsLayer {
            rotationZ = swing.value
            transformOrigin = PendulumPivot
        },
        targetState = subscribed,
        transitionSpec = {
            SubscriptionContentTransform
        },
    ) { isSubscribed ->
        Icon(
            modifier = Modifier
                .size(iconSize),
            imageVector =
            if (isSubscribed) Icons.Rounded.NotificationsActive
            else Icons.Rounded.NotificationsOff,
            contentDescription = stringResource(
                if (isSubscribed) CommonStrings.standard_publication_unsubscribe_from
                else CommonStrings.standard_publication_subscribe_to,
            ),
            tint =
            if (isSubscribed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        )
    }

    LaunchedEffect(subscribed) {
        if (initial.value) {
            initial.value = false
            return@LaunchedEffect
        }
        swing.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                0f at 0
                (-18f) at 100
                14f at 220
                (-8f) at 330
                4f at 420
                0f at 500
            },
        )
    }
}

private val SubscriptionContentTransform = fadeIn() togetherWith fadeOut()

private val PendulumPivot = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
