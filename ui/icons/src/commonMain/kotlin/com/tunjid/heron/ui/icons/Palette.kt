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

val HeronIcons.Palette: ImageVector
    get() {
        if (_palette != null) {
            return _palette!!
        }
        _palette = ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12f, 2f)
                curveTo(6.49f, 2f, 2f, 6.49f, 2f, 12f)
                reflectiveCurveToRelative(4.49f, 10f, 10f, 10f)
                curveToRelative(1.38f, 0f, 2.5f, -1.12f, 2.5f, -2.5f)
                curveToRelative(0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
                curveToRelative(-0.08f, -0.1f, -0.13f, -0.21f, -0.13f, -0.33f)
                curveToRelative(0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
                horizontalLineTo(16f)
                curveToRelative(3.31f, 0f, 6f, -2.69f, 6f, -6f)
                curveTo(22f, 6.04f, 17.51f, 2f, 12f, 2f)
                close()
                moveTo(17.5f, 13f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                curveTo(19f, 12.33f, 18.33f, 13f, 17.5f, 13f)
                close()
                moveTo(14.5f, 9f)
                curveTo(13.67f, 9f, 13f, 8.33f, 13f, 7.5f)
                curveTo(13f, 6.67f, 13.67f, 6f, 14.5f, 6f)
                reflectiveCurveTo(16f, 6.67f, 16f, 7.5f)
                curveTo(16f, 8.33f, 15.33f, 9f, 14.5f, 9f)
                close()
                moveTo(5f, 11.5f)
                curveTo(5f, 10.67f, 5.67f, 10f, 6.5f, 10f)
                reflectiveCurveTo(8f, 10.67f, 8f, 11.5f)
                curveTo(8f, 12.33f, 7.33f, 13f, 6.5f, 13f)
                reflectiveCurveTo(5f, 12.33f, 5f, 11.5f)
                close()
                moveTo(11f, 7.5f)
                curveTo(11f, 8.33f, 10.33f, 9f, 9.5f, 9f)
                reflectiveCurveTo(8f, 8.33f, 8f, 7.5f)
                curveTo(8f, 6.67f, 8.67f, 6f, 9.5f, 6f)
                reflectiveCurveTo(11f, 6.67f, 11f, 7.5f)
                close()
            }
        }.build()

        return _palette!!
    }

@Suppress("ObjectPropertyName")
private var _palette: ImageVector? = null
