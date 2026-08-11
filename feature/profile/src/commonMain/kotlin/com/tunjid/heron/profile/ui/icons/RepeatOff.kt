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

package com.tunjid.heron.profile.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The [androidx.compose.material.icons.rounded.Repeat] glyph struck through with a
 * diagonal slash, used to signal that an account's reposts are muted. The glyph and
 * the slash share a fill so [androidx.compose.material3.Icon] can tint them together,
 * while a slightly wider transparent channel keeps the slash visually separated from
 * the arrows.
 */
internal val RepeatOff: ImageVector
    get() {
        if (_RepeatOff != null) {
            return _RepeatOff!!
        }
        _RepeatOff = ImageVector.Builder(
            name = "RepeatOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Repeat arrows, with a diagonal channel cut out via the even-odd rule so
            // the slash below reads as a clean gap rather than overlapping ink.
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // Top arrow.
                moveTo(7f, 7f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(3f)
                lineToRelative(4f, -4f)
                lineToRelative(-4f, -4f)
                verticalLineToRelative(3f)
                horizontalLineTo(5f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                close()
                // Bottom arrow.
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineToRelative(-3f)
                lineToRelative(-4f, 4f)
                lineToRelative(4f, 4f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(4f)
                close()
                // Transparent channel around the slash.
                moveTo(5.2f, 3f)
                lineTo(21f, 18.8f)
                lineTo(18.8f, 21f)
                lineTo(3f, 5.2f)
                close()
            }
            // The slash itself, centered in the channel.
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(4.15f, 3f)
                lineTo(21f, 19.85f)
                lineTo(19.85f, 21f)
                lineTo(3f, 4.15f)
                close()
            }
        }.build()

        return _RepeatOff!!
    }

@Suppress("ObjectPropertyName")
private var _RepeatOff: ImageVector? = null
