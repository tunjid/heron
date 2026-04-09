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

val HeronIcons.Face: ImageVector
    get() {
        if (_face != null) {
            return _face!!
        }
        _face = ImageVector.Builder(
            name = "Face",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(10.25f, 13f)
                curveToRelative(0f, 0.69f, -0.56f, 1.25f, -1.25f, 1.25f)
                reflectiveCurveTo(7.75f, 13.69f, 7.75f, 13f)
                reflectiveCurveTo(8.31f, 11.75f, 9f, 11.75f)
                reflectiveCurveTo(10.25f, 12.31f, 10.25f, 13f)
                close()
                moveTo(15f, 11.75f)
                curveToRelative(-0.69f, 0f, -1.25f, 0.56f, -1.25f, 1.25f)
                reflectiveCurveToRelative(0.56f, 1.25f, 1.25f, 1.25f)
                reflectiveCurveToRelative(1.25f, -0.56f, 1.25f, -1.25f)
                reflectiveCurveTo(15.69f, 11.75f, 15f, 11.75f)
                close()
                moveTo(22f, 12f)
                curveToRelative(0f, 5.52f, -4.48f, 10f, -10f, 10f)
                reflectiveCurveTo(2f, 17.52f, 2f, 12f)
                reflectiveCurveTo(6.48f, 2f, 12f, 2f)
                reflectiveCurveTo(22f, 6.48f, 22f, 12f)
                close()
                moveTo(20f, 12f)
                curveToRelative(0f, -0.78f, -0.12f, -1.53f, -0.33f, -2.24f)
                curveTo(18.97f, 9.91f, 18.25f, 10f, 17.5f, 10f)
                curveToRelative(-3.13f, 0f, -5.92f, -1.44f, -7.76f, -3.69f)
                curveTo(8.69f, 8.87f, 6.6f, 10.88f, 4f, 11.86f)
                curveTo(4.01f, 11.9f, 4f, 11.95f, 4f, 12f)
                curveToRelative(0f, 4.41f, 3.59f, 8f, 8f, 8f)
                reflectiveCurveTo(20f, 16.41f, 20f, 12f)
                close()
            }
        }.build()

        return _face!!
    }

@Suppress("ObjectPropertyName")
private var _face: ImageVector? = null
