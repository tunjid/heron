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

val HeronIcons.Psychology: ImageVector
    get() {
        if (_psychology != null) {
            return _psychology!!
        }
        _psychology = ImageVector.Builder(
            name = "Psychology",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(13f, 8.57f)
                curveToRelative(-0.79f, 0f, -1.43f, 0.64f, -1.43f, 1.43f)
                reflectiveCurveToRelative(0.64f, 1.43f, 1.43f, 1.43f)
                reflectiveCurveToRelative(1.43f, -0.64f, 1.43f, -1.43f)
                reflectiveCurveTo(13.79f, 8.57f, 13f, 8.57f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(13.21f, 3f)
                curveToRelative(-3.84f, -0.11f, -7f, 2.87f, -7.19f, 6.64f)
                lineTo(4.1f, 12.2f)
                curveTo(3.85f, 12.53f, 4.09f, 13f, 4.5f, 13f)
                horizontalLineTo(6f)
                verticalLineToRelative(3f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(5f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-3.68f)
                curveToRelative(2.44f, -1.16f, 4.1f, -3.68f, 4f, -6.58f)
                curveTo(19.86f, 6.12f, 16.82f, 3.11f, 13.21f, 3f)
                close()
                moveTo(16f, 10f)
                curveToRelative(0f, 0.13f, -0.01f, 0.26f, -0.02f, 0.39f)
                lineToRelative(0.83f, 0.66f)
                curveToRelative(0.08f, 0.06f, 0.1f, 0.16f, 0.05f, 0.25f)
                lineToRelative(-0.8f, 1.39f)
                curveToRelative(-0.05f, 0.09f, -0.16f, 0.12f, -0.24f, 0.09f)
                lineToRelative(-0.99f, -0.4f)
                curveToRelative(-0.21f, 0.16f, -0.43f, 0.29f, -0.67f, 0.39f)
                lineTo(14f, 13.83f)
                curveToRelative(-0.01f, 0.1f, -0.1f, 0.17f, -0.2f, 0.17f)
                horizontalLineToRelative(-1.6f)
                curveToRelative(-0.1f, 0f, -0.18f, -0.07f, -0.2f, -0.17f)
                lineToRelative(-0.15f, -1.06f)
                curveToRelative(-0.25f, -0.1f, -0.47f, -0.23f, -0.68f, -0.39f)
                lineToRelative(-0.99f, 0.4f)
                curveToRelative(-0.09f, 0.03f, -0.2f, 0f, -0.25f, -0.09f)
                lineToRelative(-0.8f, -1.39f)
                curveToRelative(-0.05f, -0.08f, -0.03f, -0.19f, 0.05f, -0.25f)
                lineToRelative(0.84f, -0.66f)
                curveTo(10.01f, 10.26f, 10f, 10.13f, 10f, 10f)
                curveToRelative(0f, -0.13f, 0.02f, -0.27f, 0.04f, -0.39f)
                lineTo(9.19f, 8.95f)
                curveToRelative(-0.08f, -0.06f, -0.1f, -0.16f, -0.05f, -0.26f)
                lineToRelative(0.8f, -1.38f)
                curveToRelative(0.05f, -0.09f, 0.15f, -0.12f, 0.24f, -0.09f)
                lineToRelative(1f, 0.4f)
                curveToRelative(0.2f, -0.15f, 0.43f, -0.29f, 0.67f, -0.39f)
                lineToRelative(0.15f, -1.06f)
                curveTo(12.02f, 6.07f, 12.1f, 6f, 12.2f, 6f)
                horizontalLineToRelative(1.6f)
                curveToRelative(0.1f, 0f, 0.18f, 0.07f, 0.2f, 0.17f)
                lineToRelative(0.15f, 1.06f)
                curveToRelative(0.24f, 0.1f, 0.46f, 0.23f, 0.67f, 0.39f)
                lineToRelative(1f, -0.4f)
                curveToRelative(0.09f, -0.03f, 0.2f, 0f, 0.24f, 0.09f)
                lineToRelative(0.8f, 1.38f)
                curveToRelative(0.05f, 0.09f, 0.03f, 0.2f, -0.05f, 0.26f)
                lineToRelative(-0.85f, 0.66f)
                curveTo(15.99f, 9.73f, 16f, 9.86f, 16f, 10f)
                close()
            }
        }.build()

        return _psychology!!
    }

@Suppress("ObjectPropertyName")
private var _psychology: ImageVector? = null
