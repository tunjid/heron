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
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Blacksky: ImageVector
    get() {
        if (_Blacksky != null) {
            return _Blacksky!!
        }
        _Blacksky = ImageVector.Builder(
            name = "Blacksky",
            defaultWidth = 285.dp,
            defaultHeight = 243.dp,
            viewportWidth = 285f,
            viewportHeight = 243f,
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(285f)
                    verticalLineToRelative(243f)
                    horizontalLineToRelative(-285f)
                    close()
                },
            ) {
                path(fill = SolidColor(Color(0xFFF8FAF9))) {
                    moveTo(148.85f, 144.56f)
                    curveTo(148.85f, 159.75f, 161.16f, 172.06f, 176.35f, 172.06f)
                    horizontalLineTo(207.01f)
                    verticalLineTo(185.87f)
                    horizontalLineTo(176.35f)
                    curveTo(161.16f, 185.87f, 148.85f, 198.18f, 148.85f, 213.37f)
                    verticalLineTo(243.04f)
                    horizontalLineTo(136.03f)
                    verticalLineTo(213.37f)
                    curveTo(136.03f, 198.18f, 123.72f, 185.87f, 108.53f, 185.87f)
                    horizontalLineTo(77.86f)
                    verticalLineTo(172.06f)
                    horizontalLineTo(108.53f)
                    curveTo(123.72f, 172.06f, 136.03f, 159.75f, 136.03f, 144.56f)
                    verticalLineTo(113.9f)
                    horizontalLineTo(148.85f)
                    verticalLineTo(144.56f)
                    close()
                }
                path(fill = SolidColor(Color(0xFFF8FAF9))) {
                    moveTo(170.95f, 31.88f)
                    curveTo(160.21f, 42.62f, 160.21f, 60.03f, 170.95f, 70.77f)
                    lineTo(192.63f, 92.45f)
                    lineTo(182.87f, 102.21f)
                    lineTo(161.19f, 80.53f)
                    curveTo(150.45f, 69.79f, 133.04f, 69.79f, 122.3f, 80.53f)
                    lineTo(101.31f, 101.51f)
                    lineTo(92.25f, 92.45f)
                    lineTo(113.23f, 71.46f)
                    curveTo(123.97f, 60.72f, 123.97f, 43.31f, 113.23f, 32.57f)
                    lineTo(91.55f, 10.89f)
                    lineTo(101.31f, 1.13f)
                    lineTo(122.99f, 22.81f)
                    curveTo(133.73f, 33.55f, 151.14f, 33.55f, 161.88f, 22.81f)
                    lineTo(183.57f, 1.13f)
                    lineTo(192.63f, 10.19f)
                    lineTo(170.95f, 31.88f)
                    close()
                }
                path(fill = SolidColor(Color(0xFFF8FAF9))) {
                    moveTo(79.05f, 75.33f)
                    curveTo(75.12f, 90f, 83.83f, 105.08f, 98.5f, 109.01f)
                    lineTo(128.12f, 116.94f)
                    lineTo(124.55f, 130.27f)
                    lineTo(94.93f, 122.34f)
                    curveTo(80.26f, 118.41f, 65.18f, 127.11f, 61.25f, 141.78f)
                    lineTo(53.56f, 170.45f)
                    lineTo(41.18f, 167.14f)
                    lineTo(48.87f, 138.47f)
                    curveTo(52.8f, 123.8f, 44.09f, 108.72f, 29.42f, 104.79f)
                    lineTo(-0.2f, 96.85f)
                    lineTo(3.37f, 83.52f)
                    lineTo(32.99f, 91.45f)
                    curveTo(47.66f, 95.39f, 62.74f, 86.68f, 66.67f, 72.01f)
                    lineTo(74.61f, 42.39f)
                    lineTo(86.99f, 45.7f)
                    lineTo(79.05f, 75.33f)
                    close()
                }
                path(fill = SolidColor(Color(0xFFF8FAF9))) {
                    moveTo(218.41f, 71.42f)
                    curveTo(222.34f, 86.09f, 237.42f, 94.8f, 252.09f, 90.87f)
                    lineTo(281.71f, 82.93f)
                    lineTo(285.29f, 96.26f)
                    lineTo(255.67f, 104.2f)
                    curveTo(240.99f, 108.13f, 232.29f, 123.21f, 236.22f, 137.88f)
                    lineTo(243.9f, 166.55f)
                    lineTo(231.52f, 169.87f)
                    lineTo(223.84f, 141.2f)
                    curveTo(219.91f, 126.53f, 204.83f, 117.82f, 190.16f, 121.75f)
                    lineTo(160.54f, 129.69f)
                    lineTo(156.97f, 116.36f)
                    lineTo(186.59f, 108.42f)
                    curveTo(201.26f, 104.49f, 209.96f, 89.41f, 206.03f, 74.74f)
                    lineTo(198.1f, 45.12f)
                    lineTo(210.48f, 41.8f)
                    lineTo(218.41f, 71.42f)
                    close()
                }
            }
        }.build()

        return _Blacksky!!
    }

@Suppress("ObjectPropertyName")
private var _Blacksky: ImageVector? = null
