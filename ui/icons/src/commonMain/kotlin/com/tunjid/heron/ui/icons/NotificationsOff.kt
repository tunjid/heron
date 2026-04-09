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

val HeronIcons.NotificationsOff: ImageVector
    get() {
        if (_notificationsOff != null) {
            return _notificationsOff!!
        }
        _notificationsOff = ImageVector.Builder(
            name = "NotificationsOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(12f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                close()
                moveTo(18f, 11f)
                curveToRelative(0f, -3.07f, -1.64f, -5.64f, -4.5f, -6.32f)
                lineTo(13.5f, 4f)
                curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(0.68f)
                curveToRelative(-0.24f, 0.06f, -0.47f, 0.15f, -0.69f, 0.23f)
                lineTo(18f, 13.1f)
                lineTo(18f, 11f)
                close()
                moveTo(5.41f, 3.35f)
                lineTo(4f, 4.76f)
                lineToRelative(2.81f, 2.81f)
                curveTo(6.29f, 8.57f, 6f, 9.73f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-1.29f, 1.29f)
                curveToRelative(-0.63f, 0.63f, -0.19f, 1.71f, 0.7f, 1.71f)
                horizontalLineToRelative(12.83f)
                lineToRelative(1.74f, 1.74f)
                lineToRelative(1.41f, -1.41f)
                lineTo(5.41f, 3.35f)
                close()
            }
        }.build()

        return _notificationsOff!!
    }

@Suppress("ObjectPropertyName")
private var _notificationsOff: ImageVector? = null
