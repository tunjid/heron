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

val HeronIcons.Diversity3: ImageVector
    get() {
        if (_diversity3 != null) {
            return _diversity3!!
        }
        _diversity3 = ImageVector.Builder(
            name = "Diversity3",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(6.32f, 13.01f)
                curveToRelative(0.96f, 0.02f, 1.85f, 0.5f, 2.45f, 1.34f)
                curveTo(9.5f, 15.38f, 10.71f, 16f, 12f, 16f)
                curveToRelative(1.29f, 0f, 2.5f, -0.62f, 3.23f, -1.66f)
                curveToRelative(0.6f, -0.84f, 1.49f, -1.32f, 2.45f, -1.34f)
                curveTo(16.96f, 11.78f, 14.08f, 11f, 12f, 11f)
                curveTo(9.93f, 11f, 7.04f, 11.78f, 6.32f, 13.01f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(4f, 13f)
                lineTo(4f, 13f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
                reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
                curveTo(1f, 11.66f, 2.34f, 13f, 4f, 13f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(20f, 13f)
                lineTo(20f, 13f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
                reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
                curveTo(17f, 11.66f, 18.34f, 13f, 20f, 13f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12f, 10f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
                reflectiveCurveTo(9f, 5.34f, 9f, 7f)
                curveTo(9f, 8.66f, 10.34f, 10f, 12f, 10f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(21f, 14f)
                horizontalLineToRelative(-3.27f)
                curveToRelative(-0.77f, 0f, -1.35f, 0.45f, -1.68f, 0.92f)
                curveTo(16.01f, 14.98f, 14.69f, 17f, 12f, 17f)
                curveToRelative(-1.43f, 0f, -3.03f, -0.64f, -4.05f, -2.08f)
                curveTo(7.56f, 14.37f, 6.95f, 14f, 6.27f, 14f)
                lineTo(3f, 14f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(3f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(5f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-1.26f)
                curveToRelative(1.15f, 0.8f, 2.54f, 1.26f, 4f, 1.26f)
                reflectiveCurveToRelative(2.85f, -0.46f, 4f, -1.26f)
                verticalLineTo(19f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(5f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-3f)
                curveTo(23f, 14.9f, 22.1f, 14f, 21f, 14f)
                close()
            }
        }.build()

        return _diversity3!!
    }

@Suppress("ObjectPropertyName")
private var _diversity3: ImageVector? = null
