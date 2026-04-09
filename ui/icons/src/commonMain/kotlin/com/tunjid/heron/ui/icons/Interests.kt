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

val HeronIcons.Interests: ImageVector
    get() {
        if (_interests != null) {
            return _interests!!
        }
        _interests = ImageVector.Builder(
            name = "Interests",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(7.02f, 13f)
                curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
                reflectiveCurveTo(9.23f, 13f, 7.02f, 13f)
                close()
                moveTo(13f, 14f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(6f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-6f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                horizontalLineToRelative(-6f)
                curveTo(13.45f, 13f, 13f, 13.45f, 13f, 14f)
                close()
                moveTo(6.13f, 3.57f)
                lineToRelative(-3.3f, 5.94f)
                curveTo(2.46f, 10.18f, 2.94f, 11f, 3.7f, 11f)
                horizontalLineToRelative(6.6f)
                curveToRelative(0.76f, 0f, 1.24f, -0.82f, 0.87f, -1.49f)
                lineToRelative(-3.3f, -5.94f)
                curveTo(7.49f, 2.89f, 6.51f, 2.89f, 6.13f, 3.57f)
                close()
                moveTo(19.25f, 2.5f)
                curveToRelative(-1.06f, 0f, -1.81f, 0.56f, -2.25f, 1.17f)
                curveToRelative(-0.44f, -0.61f, -1.19f, -1.17f, -2.25f, -1.17f)
                curveTo(13.19f, 2.5f, 12f, 3.78f, 12f, 5.25f)
                curveToRelative(0f, 1.83f, 2.03f, 3.17f, 4.35f, 5.18f)
                curveToRelative(0.37f, 0.32f, 0.92f, 0.32f, 1.3f, 0f)
                curveTo(19.97f, 8.42f, 22f, 7.08f, 22f, 5.25f)
                curveTo(22f, 3.78f, 20.81f, 2.5f, 19.25f, 2.5f)
                close()
            }
        }.build()

        return _interests!!
    }

@Suppress("ObjectPropertyName")
private var _interests: ImageVector? = null
