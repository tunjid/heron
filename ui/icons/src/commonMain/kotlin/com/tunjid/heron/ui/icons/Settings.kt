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

val HeronIcons.Settings: ImageVector
    get() {
        if (_settings != null) {
            return _settings!!
        }
        _settings = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(19.5f, 12f)
                curveToRelative(0f, -0.23f, -0.01f, -0.45f, -0.03f, -0.68f)
                lineToRelative(1.86f, -1.41f)
                curveToRelative(0.4f, -0.3f, 0.51f, -0.86f, 0.26f, -1.3f)
                lineToRelative(-1.87f, -3.23f)
                curveToRelative(-0.25f, -0.44f, -0.79f, -0.62f, -1.25f, -0.42f)
                lineToRelative(-2.15f, 0.91f)
                curveToRelative(-0.37f, -0.26f, -0.76f, -0.49f, -1.17f, -0.68f)
                lineToRelative(-0.29f, -2.31f)
                curveTo(14.8f, 2.38f, 14.37f, 2f, 13.87f, 2f)
                horizontalLineToRelative(-3.73f)
                curveTo(9.63f, 2f, 9.2f, 2.38f, 9.14f, 2.88f)
                lineTo(8.85f, 5.19f)
                curveToRelative(-0.41f, 0.19f, -0.8f, 0.42f, -1.17f, 0.68f)
                lineTo(5.53f, 4.96f)
                curveToRelative(-0.46f, -0.2f, -1f, -0.02f, -1.25f, 0.42f)
                lineTo(2.41f, 8.62f)
                curveToRelative(-0.25f, 0.44f, -0.14f, 0.99f, 0.26f, 1.3f)
                lineToRelative(1.86f, 1.41f)
                curveTo(4.51f, 11.55f, 4.5f, 11.77f, 4.5f, 12f)
                reflectiveCurveToRelative(0.01f, 0.45f, 0.03f, 0.68f)
                lineToRelative(-1.86f, 1.41f)
                curveToRelative(-0.4f, 0.3f, -0.51f, 0.86f, -0.26f, 1.3f)
                lineToRelative(1.87f, 3.23f)
                curveToRelative(0.25f, 0.44f, 0.79f, 0.62f, 1.25f, 0.42f)
                lineToRelative(2.15f, -0.91f)
                curveToRelative(0.37f, 0.26f, 0.76f, 0.49f, 1.17f, 0.68f)
                lineToRelative(0.29f, 2.31f)
                curveTo(9.2f, 21.62f, 9.63f, 22f, 10.13f, 22f)
                horizontalLineToRelative(3.73f)
                curveToRelative(0.5f, 0f, 0.93f, -0.38f, 0.99f, -0.88f)
                lineToRelative(0.29f, -2.31f)
                curveToRelative(0.41f, -0.19f, 0.8f, -0.42f, 1.17f, -0.68f)
                lineToRelative(2.15f, 0.91f)
                curveToRelative(0.46f, 0.2f, 1f, 0.02f, 1.25f, -0.42f)
                lineToRelative(1.87f, -3.23f)
                curveToRelative(0.25f, -0.44f, 0.14f, -0.99f, -0.26f, -1.3f)
                lineToRelative(-1.86f, -1.41f)
                curveTo(19.49f, 12.45f, 19.5f, 12.23f, 19.5f, 12f)
                close()
                moveTo(12.04f, 15.5f)
                curveToRelative(-1.93f, 0f, -3.5f, -1.57f, -3.5f, -3.5f)
                reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
                reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                reflectiveCurveTo(13.97f, 15.5f, 12.04f, 15.5f)
                close()
            }
        }.build()

        return _settings!!
    }

@Suppress("ObjectPropertyName")
private var _settings: ImageVector? = null
