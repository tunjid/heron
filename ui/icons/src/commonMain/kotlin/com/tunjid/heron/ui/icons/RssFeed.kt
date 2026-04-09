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

val HeronIcons.RssFeed: ImageVector
    get() {
        if (_rssFeed != null) {
            return _rssFeed!!
        }
        _rssFeed = ImageVector.Builder(
            name = "RssFeed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(6.18f, 17.82f)
                moveToRelative(-2.18f, 0f)
                arcToRelative(2.18f, 2.18f, 0f, true, true, 4.36f, 0f)
                arcToRelative(2.18f, 2.18f, 0f, true, true, -4.36f, 0f)
            }
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(5.59f, 10.23f)
                curveToRelative(-0.84f, -0.14f, -1.59f, 0.55f, -1.59f, 1.4f)
                curveToRelative(0f, 0.71f, 0.53f, 1.28f, 1.23f, 1.4f)
                curveToRelative(2.92f, 0.51f, 5.22f, 2.82f, 5.74f, 5.74f)
                curveToRelative(0.12f, 0.7f, 0.69f, 1.23f, 1.4f, 1.23f)
                curveToRelative(0.85f, 0f, 1.54f, -0.75f, 1.41f, -1.59f)
                curveToRelative(-0.68f, -4.2f, -3.99f, -7.51f, -8.19f, -8.18f)
                close()
                moveTo(5.56f, 4.52f)
                curveTo(4.73f, 4.43f, 4f, 5.1f, 4f, 5.93f)
                curveToRelative(0f, 0.73f, 0.55f, 1.33f, 1.27f, 1.4f)
                curveToRelative(6.01f, 0.6f, 10.79f, 5.38f, 11.39f, 11.39f)
                curveToRelative(0.07f, 0.73f, 0.67f, 1.28f, 1.4f, 1.28f)
                curveToRelative(0.84f, 0f, 1.5f, -0.73f, 1.42f, -1.56f)
                curveToRelative(-0.73f, -7.34f, -6.57f, -13.19f, -13.92f, -13.92f)
                close()
            }
        }.build()

        return _rssFeed!!
    }

@Suppress("ObjectPropertyName")
private var _rssFeed: ImageVector? = null
