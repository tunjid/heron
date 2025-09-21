/*
 *    Copyright 2024 Adetunji Dahunsi
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

package com.tunjid.heron.signin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Bluesky: ImageVector
    get() {
        if (_Bluesky != null) {
            return _Bluesky!!
        }
        _Bluesky = ImageVector.Builder(
            name = "Bluesky",
            defaultWidth = 64.dp,
            defaultHeight = 57.dp,
            viewportWidth = 64f,
            viewportHeight = 57f
        ).apply {
            path(fill = SolidColor(Color(0xFF0085FF))) {
                moveTo(13.873f, 3.805f)
                curveTo(21.21f, 9.332f, 29.103f, 20.537f, 32f, 26.55f)
                verticalLineToRelative(15.882f)
                curveToRelative(0f, -0.338f, -0.13f, 0.044f, -0.41f, 0.867f)
                curveToRelative(-1.512f, 4.456f, -7.418f, 21.847f, -20.923f, 7.944f)
                curveToRelative(-7.111f, -7.32f, -3.819f, -14.64f, 9.125f, -16.85f)
                curveToRelative(-7.405f, 1.264f, -15.73f, -0.825f, -18.014f, -9.015f)
                curveTo(1.12f, 23.022f, 0f, 8.51f, 0f, 6.55f)
                curveTo(0f, -3.268f, 8.579f, -0.182f, 13.873f, 3.805f)
                close()
                moveTo(50.127f, 3.805f)
                curveTo(42.79f, 9.332f, 34.897f, 20.537f, 32f, 26.55f)
                verticalLineToRelative(15.882f)
                curveToRelative(0f, -0.338f, 0.13f, 0.044f, 0.41f, 0.867f)
                curveToRelative(1.512f, 4.456f, 7.418f, 21.847f, 20.923f, 7.944f)
                curveToRelative(7.111f, -7.32f, 3.819f, -14.64f, -9.125f, -16.85f)
                curveToRelative(7.405f, 1.264f, 15.73f, -0.825f, 18.014f, -9.015f)
                curveTo(62.88f, 23.022f, 64f, 8.51f, 64f, 6.55f)
                curveToRelative(0f, -9.818f, -8.578f, -6.732f, -13.873f, -2.745f)
                close()
            }
        }.build()

        return _Bluesky!!
    }

@Suppress("ObjectPropertyName")
private var _Bluesky: ImageVector? = null
