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

val HeronIcons.Mail: ImageVector
    get() {
        if (_mail != null) {
            return _mail!!
        }
        _mail = ImageVector.Builder(
            name = "Mail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(20f, 4f)
                lineTo(4f, 4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(22f, 6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19.6f, 8.25f)
                lineToRelative(-6.54f, 4.09f)
                curveToRelative(-0.65f, 0.41f, -1.47f, 0.41f, -2.12f, 0f)
                lineTo(4.4f, 8.25f)
                curveToRelative(-0.25f, -0.16f, -0.4f, -0.43f, -0.4f, -0.72f)
                curveToRelative(0f, -0.67f, 0.73f, -1.07f, 1.3f, -0.72f)
                lineTo(12f, 11f)
                lineToRelative(6.7f, -4.19f)
                curveToRelative(0.57f, -0.35f, 1.3f, 0.05f, 1.3f, 0.72f)
                curveToRelative(0f, 0.29f, -0.15f, 0.56f, -0.4f, 0.72f)
                close()
            }
        }.build()

        return _mail!!
    }

@Suppress("ObjectPropertyName")
private var _mail: ImageVector? = null
