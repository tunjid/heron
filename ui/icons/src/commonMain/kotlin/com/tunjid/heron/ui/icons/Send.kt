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

val HeronIcons.Send: ImageVector
    get() {
        if (_send != null) {
            return _send!!
        }
        _send = ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(3.4f, 20.4f)
                lineToRelative(17.45f, -7.48f)
                curveToRelative(0.81f, -0.35f, 0.81f, -1.49f, 0f, -1.84f)
                lineTo(3.4f, 3.6f)
                curveToRelative(-0.66f, -0.29f, -1.39f, 0.2f, -1.39f, 0.91f)
                lineTo(2f, 9.12f)
                curveToRelative(0f, 0.5f, 0.37f, 0.93f, 0.87f, 0.99f)
                lineTo(17f, 12f)
                lineTo(2.87f, 13.88f)
                curveToRelative(-0.5f, 0.07f, -0.87f, 0.5f, -0.87f, 1f)
                lineToRelative(0.01f, 4.61f)
                curveToRelative(0f, 0.71f, 0.73f, 1.2f, 1.39f, 0.91f)
                close()
            }
        }.build()

        return _send!!
    }

@Suppress("ObjectPropertyName")
private var _send: ImageVector? = null
