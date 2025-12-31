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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.tunjid.heron.scaffold.notifications.notificationPermissionsLauncher
import com.tunjid.heron.ui.AppBarButton
import heron.feature.notifications.generated.resources.Res
import heron.feature.notifications.generated.resources.notification_permissions_request
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RequestNotificationsButton(
    animateIcon: Boolean,
) {
    val ringProgressAnimatable = remember {
        Animatable(initialValue = AnimationStart)
    }

    val animationSpec = remember {
        tween<Float>(
            durationMillis = AnimationDurationMillis,
            easing = LinearEasing,
        )
    }

    AppBarButton(
        modifier = Modifier
            .graphicsLayer {
                rotationZ = calculateDampedBellRotation(ringProgressAnimatable.value)
            },
        icon = Icons.Rounded.NotificationsOff,
        iconDescription = stringResource(Res.string.notification_permissions_request),
        onClick = notificationPermissionsLauncher(),
    )

    LaunchedEffect(animateIcon) {
        if (animateIcon) ringProgressAnimatable.animateTo(
            targetValue = AnimationEnd,
            animationSpec = animationSpec,
        )
        else ringProgressAnimatable.snapTo(AnimationStart)
    }
}

private fun calculateDampedBellRotation(progress: Float): Float {
    if (progress >= 1f) return 0f

    // Uses equation of a damped oscillating spring
    return MaxRotationDegrees * exp(-AnimationDecay * progress) * sin(progress * 2 * PI * AnimationFrequency).toFloat()
}

private const val AnimationStart = 0f
private const val AnimationEnd = 1f
private const val AnimationDecay = 3f
private const val AnimationFrequency = 5f
private const val MaxRotationDegrees = 30F
private const val AnimationDurationMillis = 1500 // 3000 was too slow; 1.5s is snappier
