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

val HeronIcons.FilterAlt: ImageVector
    get() {
        if (_filterAlt != null) {
            return _filterAlt!!
        }
        _filterAlt = ImageVector.Builder(
            name = "FilterAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(4.25f, 5.61f)
                curveTo(6.57f, 8.59f, 10f, 13f, 10f, 13f)
                verticalLineToRelative(5f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(0f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-5f)
                curveToRelative(0f, 0f, 3.43f, -4.41f, 5.75f, -7.39f)
                curveTo(20.26f, 4.95f, 19.79f, 4f, 18.95f, 4f)
                horizontalLineTo(5.04f)
                curveTo(4.21f, 4f, 3.74f, 4.95f, 4.25f, 5.61f)
                close()
            }
        }.build()

        return _filterAlt!!
    }

@Suppress("ObjectPropertyName")
private var _filterAlt: ImageVector? = null
