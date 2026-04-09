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

val HeronIcons.LocalFireDepartmentOutlined: ImageVector
    get() {
        if (_localFireDepartmentOutlined != null) {
            return _localFireDepartmentOutlined!!
        }
        _localFireDepartmentOutlined = ImageVector.Builder(
            name = "LocalFireDepartmentOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(16f, 6f)
                lineToRelative(-0.44f, 0.55f)
                curveToRelative(-0.42f, 0.52f, -0.98f, 0.75f, -1.54f, 0.75f)
                curveTo(13f, 7.3f, 12f, 6.52f, 12f, 5.3f)
                verticalLineTo(2f)
                curveToRelative(0f, 0f, -8f, 4f, -8f, 11f)
                curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
                reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
                curveTo(20f, 10.04f, 18.39f, 7.38f, 16f, 6f)
                close()
                moveTo(12f, 19f)
                curveToRelative(-1.1f, 0f, -2f, -0.87f, -2f, -1.94f)
                curveToRelative(0f, -0.51f, 0.2f, -0.99f, 0.58f, -1.36f)
                lineToRelative(1.42f, -1.4f)
                lineToRelative(1.43f, 1.4f)
                curveTo(13.8f, 16.07f, 14f, 16.55f, 14f, 17.06f)
                curveTo(14f, 18.13f, 13.1f, 19f, 12f, 19f)
                close()
                moveTo(15.96f, 17.5f)
                lineTo(15.96f, 17.5f)
                curveToRelative(0.04f, -0.36f, 0.22f, -1.89f, -1.13f, -3.22f)
                lineToRelative(0f, 0f)
                lineTo(12f, 11.5f)
                lineToRelative(-2.83f, 2.78f)
                lineToRelative(0f, 0f)
                curveToRelative(-1.36f, 1.34f, -1.17f, 2.88f, -1.13f, 3.22f)
                curveTo(6.79f, 16.4f, 6f, 14.79f, 6f, 13f)
                curveToRelative(0f, -3.16f, 2.13f, -5.65f, 4.03f, -7.25f)
                curveToRelative(0.23f, 1.99f, 1.93f, 3.55f, 3.99f, 3.55f)
                curveToRelative(0.78f, 0f, 1.54f, -0.23f, 2.18f, -0.66f)
                curveTo(17.34f, 9.78f, 18f, 11.35f, 18f, 13f)
                curveTo(18f, 14.79f, 17.21f, 16.4f, 15.96f, 17.5f)
                close()
            }
        }.build()

        return _localFireDepartmentOutlined!!
    }

@Suppress("ObjectPropertyName")
private var _localFireDepartmentOutlined: ImageVector? = null
