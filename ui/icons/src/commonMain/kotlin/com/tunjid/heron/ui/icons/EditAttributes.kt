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

val HeronIcons.EditAttributes: ImageVector
    get() {
        if (_editAttributes != null) {
            return _editAttributes!!
        }
        _editAttributes = ImageVector.Builder(
            name = "EditAttributes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(17.63f, 7f)
                lineTo(6.37f, 7f)
                curveTo(3.96f, 7f, 2f, 9.24f, 2f, 12f)
                reflectiveCurveToRelative(1.96f, 5f, 4.37f, 5f)
                horizontalLineToRelative(11.26f)
                curveToRelative(2.41f, 0f, 4.37f, -2.24f, 4.37f, -5f)
                reflectiveCurveToRelative(-1.96f, -5f, -4.37f, -5f)
                close()
                moveTo(11.11f, 10.6f)
                lineTo(7.6f, 14.11f)
                curveToRelative(-0.1f, 0.1f, -0.23f, 0.15f, -0.35f, 0.15f)
                reflectiveCurveToRelative(-0.26f, -0.05f, -0.35f, -0.15f)
                lineToRelative(-1.86f, -1.86f)
                curveToRelative(-0.2f, -0.2f, -0.2f, -0.51f, 0f, -0.71f)
                reflectiveCurveToRelative(0.51f, -0.2f, 0.71f, 0f)
                lineToRelative(1.51f, 1.51f)
                lineToRelative(3.16f, -3.16f)
                curveToRelative(0.2f, -0.2f, 0.51f, -0.2f, 0.71f, 0f)
                reflectiveCurveToRelative(0.17f, 0.51f, -0.02f, 0.71f)
                close()
            }
        }.build()

        return _editAttributes!!
    }

@Suppress("ObjectPropertyName")
private var _editAttributes: ImageVector? = null
