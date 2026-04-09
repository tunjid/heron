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

val HeronIcons.VolumeUp: ImageVector
    get() {
        if (_volumeUp != null) {
            return _volumeUp!!
        }
        _volumeUp = ImageVector.Builder(
            name = "VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(3f, 10f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(3f)
                lineToRelative(3.29f, 3.29f)
                curveToRelative(0.63f, 0.63f, 1.71f, 0.18f, 1.71f, -0.71f)
                lineTo(12f, 6.41f)
                curveToRelative(0f, -0.89f, -1.08f, -1.34f, -1.71f, -0.71f)
                lineTo(7f, 9f)
                lineTo(4f, 9f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                close()
                moveTo(16.5f, 12f)
                curveToRelative(0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
                close()
                moveTo(14f, 4.45f)
                verticalLineToRelative(0.2f)
                curveToRelative(0f, 0.38f, 0.25f, 0.71f, 0.6f, 0.85f)
                curveTo(17.18f, 6.53f, 19f, 9.06f, 19f, 12f)
                reflectiveCurveToRelative(-1.82f, 5.47f, -4.4f, 6.5f)
                curveToRelative(-0.36f, 0.14f, -0.6f, 0.47f, -0.6f, 0.85f)
                verticalLineToRelative(0.2f)
                curveToRelative(0f, 0.63f, 0.63f, 1.07f, 1.21f, 0.85f)
                curveTo(18.6f, 19.11f, 21f, 15.84f, 21f, 12f)
                reflectiveCurveToRelative(-2.4f, -7.11f, -5.79f, -8.4f)
                curveToRelative(-0.58f, -0.23f, -1.21f, 0.22f, -1.21f, 0.85f)
                close()
            }
        }.build()

        return _volumeUp!!
    }

@Suppress("ObjectPropertyName")
private var _volumeUp: ImageVector? = null
