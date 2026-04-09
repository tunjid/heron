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

val HeronIcons.AlternateEmail: ImageVector
    get() {
        if (_alternateEmail != null) {
            return _alternateEmail!!
        }
        _alternateEmail = ImageVector.Builder(
            name = "AlternateEmail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12.72f, 2.03f)
                curveTo(6.63f, 1.6f, 1.6f, 6.63f, 2.03f, 12.72f)
                curveTo(2.39f, 18.01f, 7.01f, 22f, 12.31f, 22f)
                horizontalLineTo(16f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
                horizontalLineToRelative(-3.67f)
                curveToRelative(-3.73f, 0f, -7.15f, -2.42f, -8.08f, -6.03f)
                curveToRelative(-1.49f, -5.8f, 3.91f, -11.21f, 9.71f, -9.71f)
                curveTo(17.58f, 5.18f, 20f, 8.6f, 20f, 12.33f)
                verticalLineToRelative(1.1f)
                curveToRelative(0f, 0.79f, -0.71f, 1.57f, -1.5f, 1.57f)
                reflectiveCurveToRelative(-1.5f, -0.78f, -1.5f, -1.57f)
                verticalLineToRelative(-1.25f)
                curveToRelative(0f, -2.51f, -1.78f, -4.77f, -4.26f, -5.12f)
                curveToRelative(-3.4f, -0.49f, -6.27f, 2.45f, -5.66f, 5.87f)
                curveToRelative(0.34f, 1.91f, 1.83f, 3.49f, 3.72f, 3.94f)
                curveToRelative(1.84f, 0.43f, 3.59f, -0.16f, 4.74f, -1.33f)
                curveToRelative(0.89f, 1.22f, 2.67f, 1.86f, 4.3f, 1.21f)
                curveToRelative(1.34f, -0.53f, 2.16f, -1.9f, 2.16f, -3.34f)
                verticalLineToRelative(-1.09f)
                curveToRelative(0f, -5.31f, -3.99f, -9.93f, -9.28f, -10.29f)
                close()
                moveTo(12f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
                reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
                close()
            }
        }.build()

        return _alternateEmail!!
    }

@Suppress("ObjectPropertyName")
private var _alternateEmail: ImageVector? = null
