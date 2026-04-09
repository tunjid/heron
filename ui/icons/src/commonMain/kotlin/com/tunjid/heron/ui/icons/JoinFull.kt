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

val HeronIcons.JoinFull: ImageVector
    get() {
        if (_joinFull != null) {
            return _joinFull!!
        }
        _joinFull = ImageVector.Builder(
            name = "JoinFull",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12.68f, 6.8f)
                curveToRelative(-0.39f, -0.35f, -0.98f, -0.35f, -1.37f, 0f)
                curveTo(9.35f, 8.56f, 9f, 10.84f, 9f, 12f)
                curveToRelative(0f, 1.15f, 0.35f, 3.44f, 2.32f, 5.2f)
                curveToRelative(0.39f, 0.35f, 0.98f, 0.35f, 1.37f, 0f)
                curveTo(14.65f, 15.44f, 15f, 13.16f, 15f, 12f)
                curveTo(15f, 10.85f, 14.65f, 8.56f, 12.68f, 6.8f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(7.5f, 12f)
                curveToRelative(0f, -0.97f, 0.23f, -4.16f, 3.03f, -6.5f)
                curveTo(9.75f, 5.19f, 8.9f, 5f, 8f, 5f)
                curveToRelative(-3.86f, 0f, -7f, 3.14f, -7f, 7f)
                reflectiveCurveToRelative(3.14f, 7f, 7f, 7f)
                curveToRelative(0.9f, 0f, 1.75f, -0.19f, 2.53f, -0.5f)
                curveTo(7.73f, 16.16f, 7.5f, 12.97f, 7.5f, 12f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(16f, 5f)
                curveToRelative(-0.9f, 0f, -1.75f, 0.19f, -2.53f, 0.5f)
                curveToRelative(2.8f, 2.34f, 3.03f, 5.53f, 3.03f, 6.5f)
                curveToRelative(0f, 0.97f, -0.23f, 4.16f, -3.03f, 6.5f)
                curveTo(14.25f, 18.81f, 15.1f, 19f, 16f, 19f)
                curveToRelative(3.86f, 0f, 7f, -3.14f, 7f, -7f)
                reflectiveCurveTo(19.86f, 5f, 16f, 5f)
                close()
            }
        }.build()

        return _joinFull!!
    }

@Suppress("ObjectPropertyName")
private var _joinFull: ImageVector? = null
