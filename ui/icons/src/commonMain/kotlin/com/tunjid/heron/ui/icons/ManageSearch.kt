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

val HeronIcons.ManageSearch: ImageVector
    get() {
        if (_manageSearch != null) {
            return _manageSearch!!
        }
        _manageSearch = ImageVector.Builder(
            name = "ManageSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(6f, 9f)
                horizontalLineTo(3f)
                curveTo(2.45f, 9f, 2f, 8.55f, 2f, 8f)
                verticalLineToRelative(0f)
                curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
                horizontalLineToRelative(3f)
                curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
                verticalLineToRelative(0f)
                curveTo(7f, 8.55f, 6.55f, 9f, 6f, 9f)
                close()
                moveTo(6f, 12f)
                horizontalLineTo(3f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(3f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(0f)
                curveTo(7f, 12.45f, 6.55f, 12f, 6f, 12f)
                close()
                moveTo(19.88f, 18.29f)
                lineToRelative(-3.12f, -3.12f)
                curveToRelative(-0.86f, 0.56f, -1.89f, 0.88f, -3f, 0.82f)
                curveToRelative(-2.37f, -0.11f, -4.4f, -1.96f, -4.72f, -4.31f)
                curveTo(8.6f, 8.33f, 11.49f, 5.5f, 14.87f, 6.07f)
                curveToRelative(1.95f, 0.33f, 3.57f, 1.85f, 4f, 3.78f)
                curveToRelative(0.33f, 1.46f, 0.01f, 2.82f, -0.7f, 3.9f)
                lineToRelative(3.13f, 3.13f)
                curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0f, 1.41f)
                lineToRelative(0f, 0f)
                curveTo(20.91f, 18.68f, 20.27f, 18.68f, 19.88f, 18.29f)
                close()
                moveTo(17f, 11f)
                curveToRelative(0f, -1.65f, -1.35f, -3f, -3f, -3f)
                reflectiveCurveToRelative(-3f, 1.35f, -3f, 3f)
                reflectiveCurveToRelative(1.35f, 3f, 3f, 3f)
                reflectiveCurveTo(17f, 12.65f, 17f, 11f)
                close()
                moveTo(3f, 19f)
                horizontalLineToRelative(8f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                horizontalLineTo(3f)
                curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                verticalLineToRelative(0f)
                curveTo(2f, 18.55f, 2.45f, 19f, 3f, 19f)
                close()
            }
        }.build()

        return _manageSearch!!
    }

@Suppress("ObjectPropertyName")
private var _manageSearch: ImageVector? = null
