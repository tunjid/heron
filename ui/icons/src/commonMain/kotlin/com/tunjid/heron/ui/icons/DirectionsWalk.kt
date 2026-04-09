/*
 *    Copyright 2026 Adetunji Dahunsi
 *    Derived from androidx.compose.material.icons — Apache License 2.0
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

package com.tunjid.heron.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HeronIcons.DirectionsWalk: ImageVector
    get() {
        if (_directionsWalk != null) {
            return _directionsWalk!!
        }
        _directionsWalk = ImageVector.Builder(
            name = "DirectionsWalk",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(13.5f, 5.5f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                close()
                moveTo(9.8f, 8.9f)
                lineTo(7.24f, 21.81f)
                curveToRelative(-0.13f, 0.61f, 0.35f, 1.19f, 0.98f, 1.19f)
                horizontalLineToRelative(0.08f)
                curveToRelative(0.47f, 0f, 0.87f, -0.32f, 0.98f, -0.78f)
                lineTo(10.9f, 15f)
                lineToRelative(2.1f, 2f)
                verticalLineToRelative(5f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-5.64f)
                curveToRelative(0f, -0.55f, -0.22f, -1.07f, -0.62f, -1.45f)
                lineTo(12.9f, 13.5f)
                lineToRelative(0.6f, -3f)
                curveToRelative(1.07f, 1.24f, 2.62f, 2.13f, 4.36f, 2.41f)
                curveToRelative(0.6f, 0.09f, 1.14f, -0.39f, 1.14f, -1f)
                curveToRelative(0f, -0.49f, -0.36f, -0.9f, -0.85f, -0.98f)
                curveToRelative(-1.52f, -0.25f, -2.78f, -1.15f, -3.45f, -2.33f)
                lineToRelative(-1f, -1.6f)
                curveToRelative(-0.56f, -0.89f, -1.68f, -1.25f, -2.65f, -0.84f)
                lineTo(7.22f, 7.78f)
                curveTo(6.48f, 8.1f, 6f, 8.82f, 6f, 9.63f)
                verticalLineTo(12f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
                verticalLineTo(9.6f)
                lineToRelative(1.8f, -0.7f)
            }
        }.build()

        return _directionsWalk!!
    }

@Suppress("ObjectPropertyName")
private var _directionsWalk: ImageVector? = null
