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

val Help: ImageVector
    get() {
        if (_Help != null) {
            return _Help!!
        }
        _Help =
            ImageVector.Builder(
                    name = "Help",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                )
                .apply {
                    path(fill = SolidColor(Color(0xFFE3E3E3))) {
                        moveTo(478f, 720f)
                        quadToRelative(21f, 0f, 35.5f, -14.5f)
                        reflectiveQuadTo(528f, 670f)
                        quadToRelative(0f, -21f, -14.5f, -35.5f)
                        reflectiveQuadTo(478f, 620f)
                        quadToRelative(-21f, 0f, -35.5f, 14.5f)
                        reflectiveQuadTo(428f, 670f)
                        quadToRelative(0f, 21f, 14.5f, 35.5f)
                        reflectiveQuadTo(478f, 720f)
                        close()
                        moveTo(442f, 566f)
                        horizontalLineToRelative(74f)
                        quadToRelative(0f, -33f, 7.5f, -52f)
                        reflectiveQuadToRelative(42.5f, -52f)
                        quadToRelative(26f, -26f, 41f, -49.5f)
                        reflectiveQuadToRelative(15f, -56.5f)
                        quadToRelative(0f, -56f, -41f, -86f)
                        reflectiveQuadToRelative(-97f, -30f)
                        quadToRelative(-57f, 0f, -92.5f, 30f)
                        reflectiveQuadTo(342f, 342f)
                        lineToRelative(66f, 26f)
                        quadToRelative(5f, -18f, 22.5f, -39f)
                        reflectiveQuadToRelative(53.5f, -21f)
                        quadToRelative(32f, 0f, 48f, 17.5f)
                        reflectiveQuadToRelative(16f, 38.5f)
                        quadToRelative(0f, 20f, -12f, 37.5f)
                        reflectiveQuadTo(506f, 434f)
                        quadToRelative(-44f, 39f, -54f, 59f)
                        reflectiveQuadToRelative(-10f, 73f)
                        close()
                        moveTo(480f, 880f)
                        quadToRelative(-83f, 0f, -156f, -31.5f)
                        reflectiveQuadTo(197f, 763f)
                        quadToRelative(-54f, -54f, -85.5f, -127f)
                        reflectiveQuadTo(80f, 480f)
                        quadToRelative(0f, -83f, 31.5f, -156f)
                        reflectiveQuadTo(197f, 197f)
                        quadToRelative(54f, -54f, 127f, -85.5f)
                        reflectiveQuadTo(480f, 80f)
                        quadToRelative(83f, 0f, 156f, 31.5f)
                        reflectiveQuadTo(763f, 197f)
                        quadToRelative(54f, 54f, 85.5f, 127f)
                        reflectiveQuadTo(880f, 480f)
                        quadToRelative(0f, 83f, -31.5f, 156f)
                        reflectiveQuadTo(763f, 763f)
                        quadToRelative(-54f, 54f, -127f, 85.5f)
                        reflectiveQuadTo(480f, 880f)
                        close()
                        moveTo(480f, 800f)
                        quadToRelative(134f, 0f, 227f, -93f)
                        reflectiveQuadToRelative(93f, -227f)
                        quadToRelative(0f, -134f, -93f, -227f)
                        reflectiveQuadToRelative(-227f, -93f)
                        quadToRelative(-134f, 0f, -227f, 93f)
                        reflectiveQuadToRelative(-93f, 227f)
                        quadToRelative(0f, 134f, 93f, 227f)
                        reflectiveQuadToRelative(227f, 93f)
                        close()
                        moveTo(480f, 480f)
                        close()
                    }
                }
                .build()

        return _Help!!
    }

@Suppress("ObjectPropertyName") private var _Help: ImageVector? = null
