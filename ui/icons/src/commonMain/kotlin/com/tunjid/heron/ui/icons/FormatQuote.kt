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

val HeronIcons.FormatQuote: ImageVector
    get() {
        if (_formatQuote != null) {
            return _formatQuote!!
        }
        _formatQuote = ImageVector.Builder(
            name = "FormatQuote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(7.17f, 17f)
                curveToRelative(0.51f, 0f, 0.98f, -0.29f, 1.2f, -0.74f)
                lineToRelative(1.42f, -2.84f)
                curveToRelative(0.14f, -0.28f, 0.21f, -0.58f, 0.21f, -0.89f)
                lineTo(10f, 8f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                lineTo(5f, 7f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(2f)
                lineToRelative(-1.03f, 2.06f)
                curveToRelative(-0.45f, 0.89f, 0.2f, 1.94f, 1.2f, 1.94f)
                close()
                moveTo(17.17f, 17f)
                curveToRelative(0.51f, 0f, 0.98f, -0.29f, 1.2f, -0.74f)
                lineToRelative(1.42f, -2.84f)
                curveToRelative(0.14f, -0.28f, 0.21f, -0.58f, 0.21f, -0.89f)
                lineTo(20f, 8f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                horizontalLineToRelative(-4f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(2f)
                lineToRelative(-1.03f, 2.06f)
                curveToRelative(-0.45f, 0.89f, 0.2f, 1.94f, 1.2f, 1.94f)
                close()
            }
        }.build()

        return _formatQuote!!
    }

@Suppress("ObjectPropertyName")
private var _formatQuote: ImageVector? = null
