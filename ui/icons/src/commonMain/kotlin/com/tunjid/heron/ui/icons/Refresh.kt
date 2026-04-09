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

val HeronIcons.Refresh: ImageVector
    get() {
        if (_refresh != null) {
            return _refresh!!
        }
        _refresh = ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(17.65f, 6.35f)
                curveToRelative(-1.63f, -1.63f, -3.94f, -2.57f, -6.48f, -2.31f)
                curveToRelative(-3.67f, 0.37f, -6.69f, 3.35f, -7.1f, 7.02f)
                curveTo(3.52f, 15.91f, 7.27f, 20f, 12f, 20f)
                curveToRelative(3.19f, 0f, 5.93f, -1.87f, 7.21f, -4.56f)
                curveToRelative(0.32f, -0.67f, -0.16f, -1.44f, -0.9f, -1.44f)
                curveToRelative(-0.37f, 0f, -0.72f, 0.2f, -0.88f, 0.53f)
                curveToRelative(-1.13f, 2.43f, -3.84f, 3.97f, -6.8f, 3.31f)
                curveToRelative(-2.22f, -0.49f, -4.01f, -2.3f, -4.48f, -4.52f)
                curveTo(5.31f, 9.44f, 8.26f, 6f, 12f, 6f)
                curveToRelative(1.66f, 0f, 3.14f, 0.69f, 4.22f, 1.78f)
                lineToRelative(-1.51f, 1.51f)
                curveToRelative(-0.63f, 0.63f, -0.19f, 1.71f, 0.7f, 1.71f)
                horizontalLineTo(19f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineTo(6.41f)
                curveToRelative(0f, -0.89f, -1.08f, -1.34f, -1.71f, -0.71f)
                lineToRelative(-0.64f, 0.65f)
                close()
            }
        }.build()

        return _refresh!!
    }

@Suppress("ObjectPropertyName")
private var _refresh: ImageVector? = null
