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

val HeronIcons.LinearScale: ImageVector
    get() {
        if (_linearScale != null) {
            return _linearScale!!
        }
        _linearScale = ImageVector.Builder(
            name = "LinearScale",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(17f, 7f)
                curveToRelative(-2.41f, 0f, -4.43f, 1.72f, -4.9f, 4f)
                horizontalLineTo(6.79f)
                curveTo(6.4f, 10.12f, 5.52f, 9.5f, 4.5f, 9.5f)
                curveTo(3.12f, 9.5f, 2f, 10.62f, 2f, 12f)
                reflectiveCurveToRelative(1.12f, 2.5f, 2.5f, 2.5f)
                curveToRelative(1.02f, 0f, 1.9f, -0.62f, 2.29f, -1.5f)
                horizontalLineToRelative(5.31f)
                curveToRelative(0.46f, 2.28f, 2.48f, 4f, 4.9f, 4f)
                curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
                reflectiveCurveTo(19.76f, 7f, 17f, 7f)
                close()
                moveTo(17f, 15f)
                curveToRelative(-1.65f, 0f, -3f, -1.35f, -3f, -3f)
                reflectiveCurveToRelative(1.35f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.35f, 3f, 3f)
                reflectiveCurveTo(18.65f, 15f, 17f, 15f)
                close()
            }
        }.build()

        return _linearScale!!
    }

@Suppress("ObjectPropertyName")
private var _linearScale: ImageVector? = null
