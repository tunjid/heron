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

val HeronIcons.Newspaper: ImageVector
    get() {
        if (_newspaper != null) {
            return _newspaper!!
        }
        _newspaper = ImageVector.Builder(
            name = "Newspaper",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(21.15f, 3.85f)
                lineToRelative(-0.82f, 0.82f)
                lineToRelative(-0.95f, -0.96f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.42f, 0f)
                lineTo(17f, 4.67f)
                lineToRelative(-0.96f, -0.96f)
                curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.42f, 0f)
                lineToRelative(-0.95f, 0.96f)
                lineToRelative(-0.96f, -0.96f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
                lineToRelative(-0.96f, 0.96f)
                lineTo(9.38f, 3.71f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.42f, 0f)
                lineTo(7f, 4.67f)
                lineTo(6.04f, 3.71f)
                curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.42f, 0f)
                lineTo(3.67f, 4.67f)
                lineTo(2.85f, 3.85f)
                curveTo(2.54f, 3.54f, 2f, 3.76f, 2f, 4.21f)
                verticalLineTo(19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                lineToRelative(16f, 0f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4.21f)
                curveTo(22f, 3.76f, 21.46f, 3.54f, 21.15f, 3.85f)
                close()
                moveTo(11f, 19f)
                horizontalLineTo(4f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(7f)
                verticalLineTo(19f)
                close()
                moveTo(20f, 19f)
                horizontalLineToRelative(-7f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(7f)
                verticalLineTo(19f)
                close()
                moveTo(20f, 15f)
                horizontalLineToRelative(-7f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(7f)
                verticalLineTo(15f)
                close()
                moveTo(20f, 11f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                horizontalLineToRelative(16f)
                verticalLineTo(11f)
                close()
            }
        }.build()

        return _newspaper!!
    }

@Suppress("ObjectPropertyName")
private var _newspaper: ImageVector? = null
