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

val HeronIcons.ForwardToInbox: ImageVector
    get() {
        if (_forwardToInbox != null) {
            return _forwardToInbox!!
        }
        _forwardToInbox = ImageVector.Builder(
            name = "ForwardToInbox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(9f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                lineToRelative(6.94f, 4.34f)
                curveToRelative(0.65f, 0.41f, 1.47f, 0.41f, 2.12f, 0f)
                lineTo(20f, 8f)
                verticalLineToRelative(5f)
                horizontalLineToRelative(2f)
                verticalLineTo(6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                close()
                moveTo(12f, 11f)
                lineTo(4f, 6f)
                horizontalLineToRelative(16f)
                lineTo(12f, 11f)
                close()
                moveTo(19f, 16.21f)
                curveToRelative(0f, -0.45f, 0.54f, -0.67f, 0.85f, -0.35f)
                lineToRelative(2.79f, 2.79f)
                curveToRelative(0.2f, 0.2f, 0.2f, 0.51f, 0f, 0.71f)
                lineToRelative(-2.79f, 2.79f)
                curveTo(19.54f, 22.46f, 19f, 22.24f, 19f, 21.79f)
                verticalLineTo(20f)
                horizontalLineToRelative(-3f)
                curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
                horizontalLineToRelative(3f)
                verticalLineTo(16.21f)
                close()
            }
        }.build()

        return _forwardToInbox!!
    }

@Suppress("ObjectPropertyName")
private var _forwardToInbox: ImageVector? = null
