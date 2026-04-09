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

val HeronIcons.Favorite: ImageVector
    get() {
        if (_favorite != null) {
            return _favorite!!
        }
        _favorite = ImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(13.35f, 20.13f)
                curveToRelative(-0.76f, 0.69f, -1.93f, 0.69f, -2.69f, -0.01f)
                lineToRelative(-0.11f, -0.1f)
                curveTo(5.3f, 15.27f, 1.87f, 12.16f, 2f, 8.28f)
                curveToRelative(0.06f, -1.7f, 0.93f, -3.33f, 2.34f, -4.29f)
                curveToRelative(2.64f, -1.8f, 5.9f, -0.96f, 7.66f, 1.1f)
                curveToRelative(1.76f, -2.06f, 5.02f, -2.91f, 7.66f, -1.1f)
                curveToRelative(1.41f, 0.96f, 2.28f, 2.59f, 2.34f, 4.29f)
                curveToRelative(0.14f, 3.88f, -3.3f, 6.99f, -8.55f, 11.76f)
                lineToRelative(-0.1f, 0.09f)
                close()
            }
        }.build()

        return _favorite!!
    }

@Suppress("ObjectPropertyName")
private var _favorite: ImageVector? = null
