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

val HeronIcons.ConnectWithoutContact: ImageVector
    get() {
        if (_connectWithoutContact != null) {
            return _connectWithoutContact!!
        }
        _connectWithoutContact = ImageVector.Builder(
            name = "ConnectWithoutContact",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(7f, 4f)
                curveToRelative(0f, -1.11f, -0.89f, -2f, -2f, -2f)
                reflectiveCurveTo(3f, 2.89f, 3f, 4f)
                reflectiveCurveToRelative(0.89f, 2f, 2f, 2f)
                reflectiveCurveTo(7f, 5.11f, 7f, 4f)
                close()
                moveTo(10.19f, 4.5f)
                lineTo(10.19f, 4.5f)
                curveToRelative(-0.41f, 0f, -0.76f, 0.25f, -0.92f, 0.63f)
                curveTo(8.83f, 6.23f, 7.76f, 7f, 6.5f, 7f)
                horizontalLineToRelative(-3f)
                curveTo(2.67f, 7f, 2f, 7.67f, 2f, 8.5f)
                verticalLineTo(11f)
                horizontalLineToRelative(6f)
                verticalLineTo(8.74f)
                curveToRelative(1.43f, -0.45f, 2.58f, -1.53f, 3.12f, -2.91f)
                curveTo(11.38f, 5.19f, 10.88f, 4.5f, 10.19f, 4.5f)
                close()
                moveTo(19f, 17f)
                curveToRelative(1.11f, 0f, 2f, -0.89f, 2f, -2f)
                reflectiveCurveToRelative(-0.89f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.89f, -2f, 2f)
                reflectiveCurveTo(17.89f, 17f, 19f, 17f)
                close()
                moveTo(20.5f, 18f)
                horizontalLineToRelative(-3f)
                curveToRelative(-1.26f, 0f, -2.33f, -0.77f, -2.77f, -1.87f)
                curveToRelative(-0.15f, -0.38f, -0.51f, -0.63f, -0.92f, -0.63f)
                horizontalLineToRelative(0f)
                curveToRelative(-0.69f, 0f, -1.19f, 0.69f, -0.94f, 1.33f)
                curveToRelative(0.55f, 1.38f, 1.69f, 2.46f, 3.12f, 2.91f)
                verticalLineTo(22f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2.5f)
                curveTo(22f, 18.67f, 21.33f, 18f, 20.5f, 18f)
                close()
                moveTo(17.25f, 11.09f)
                curveToRelative(0f, 0f, 0f, -0.01f, 0.01f, 0f)
                curveToRelative(-1.06f, 0.27f, -1.9f, 1.11f, -2.17f, 2.17f)
                curveToRelative(0f, 0f, 0f, -0.01f, 0f, -0.01f)
                curveTo(14.98f, 13.68f, 14.58f, 14f, 14.11f, 14f)
                curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
                curveToRelative(0f, -0.05f, 0.02f, -0.14f, 0.02f, -0.14f)
                curveToRelative(0.43f, -1.85f, 1.89f, -3.31f, 3.75f, -3.73f)
                curveToRelative(0.04f, 0f, 0.08f, -0.01f, 0.12f, -0.01f)
                curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
                curveTo(18f, 10.58f, 17.68f, 10.98f, 17.25f, 11.09f)
                close()
                moveTo(18f, 6.06f)
                curveToRelative(0f, 0.51f, -0.37f, 0.92f, -0.86f, 0.99f)
                curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
                curveToRelative(-3.19f, 0.39f, -5.7f, 2.91f, -6.09f, 6.1f)
                curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
                curveTo(10.98f, 13.63f, 10.56f, 14f, 10.06f, 14f)
                curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
                curveToRelative(0f, -0.02f, 0f, -0.04f, 0f, -0.06f)
                curveToRelative(0f, -0.01f, 0f, -0.02f, 0f, -0.03f)
                curveToRelative(0.5f, -4.12f, 3.79f, -7.38f, 7.92f, -7.85f)
                curveToRelative(0f, 0f, 0.01f, 0f, 0.01f, 0f)
                curveTo(17.55f, 5.06f, 18f, 5.51f, 18f, 6.06f)
                close()
            }
        }.build()

        return _connectWithoutContact!!
    }

@Suppress("ObjectPropertyName")
private var _connectWithoutContact: ImageVector? = null
