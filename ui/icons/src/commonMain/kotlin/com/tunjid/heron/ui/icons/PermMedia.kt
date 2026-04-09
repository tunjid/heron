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

val HeronIcons.PermMedia: ImageVector
    get() {
        if (_permMedia != null) {
            return _permMedia!!
        }
        _permMedia = ImageVector.Builder(
            name = "PermMedia",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(19f, 19f)
                horizontalLineTo(3f)
                verticalLineTo(7f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                reflectiveCurveTo(1f, 6.45f, 1f, 7f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                reflectiveCurveTo(19.55f, 19f, 19f, 19f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(21f, 4f)
                horizontalLineToRelative(-7f)
                lineToRelative(-1.41f, -1.41f)
                curveTo(12.21f, 2.21f, 11.7f, 2f, 11.17f, 2f)
                horizontalLineTo(7f)
                curveTo(5.9f, 2f, 5.01f, 2.9f, 5.01f, 4f)
                lineTo(5f, 15f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveTo(23f, 4.9f, 22.1f, 4f, 21f, 4f)
                close()
                moveTo(18f, 13f)
                horizontalLineToRelative(-8f)
                curveToRelative(-0.41f, 0f, -0.65f, -0.47f, -0.4f, -0.8f)
                lineToRelative(1.38f, -1.83f)
                curveToRelative(0.2f, -0.27f, 0.6f, -0.27f, 0.8f, 0f)
                lineTo(13f, 12f)
                lineToRelative(2.22f, -2.97f)
                curveToRelative(0.2f, -0.27f, 0.6f, -0.27f, 0.8f, 0f)
                lineToRelative(2.38f, 3.17f)
                curveTo(18.65f, 12.53f, 18.41f, 13f, 18f, 13f)
                close()
            }
        }.build()

        return _permMedia!!
    }

@Suppress("ObjectPropertyName")
private var _permMedia: ImageVector? = null
