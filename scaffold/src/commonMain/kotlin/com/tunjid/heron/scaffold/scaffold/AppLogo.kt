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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.PanedSharedElementScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PanedSharedElementScope.AppLogo(
    modifier: Modifier,
) {
    Icon(
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(AppLogo),
                animatedVisibilityScope = this,
                boundsTransform = { _, _ ->
                    spring(stiffness = Spring.StiffnessLow)
                }
            ),
        imageVector = AppLogo,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

val AppLogo: ImageVector
    get() {
        if (_Heron != null) {
            return _Heron!!
        }
        _Heron = ImageVector.Builder(
            name = "Heron",
            defaultWidth = 35.dp,
            defaultHeight = 48.dp,
            viewportWidth = 35f,
            viewportHeight = 48f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(0f, 12.0766f)
                lineToRelative(1.3261f, 1.7184f)
                lineToRelative(9.8007f, -7.2465f)
                lineToRelative(-1.8694f, -2.3891f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(13.4049f, 14.2816f)
                lineTo(13.4119f, 14.2755f)
                curveTo(13.41190f, 14.27550f, 20.63680f, 7.87230f, 20.94310f, 6.54070f)
                lineTo(16.4632f, 6.5407f)
                lineTo(16.4632f, 6.5407f)
                lineTo(16.458f, 6.5407f)
                lineTo(5.8086f, 15.615f)
                lineTo(5.8138f, 15.622f)
                curveTo(4.00430f, 17.10430f, 2.06120f, 19.55940f, 2.06120f, 22.10450f)
                curveTo(2.06120f, 25.91590f, 5.44320f, 28.92270f, 8.93890f, 29.77750f)
                lineTo(29.6347f, 37.4259f)
                lineTo(29.6365f, 37.4215f)
                lineTo(34.7143f, 39.6043f)
                curveTo(30.70480f, 29.20030f, 22.97650f, 19.83350f, 13.40490f, 14.28160f)
                moveTo(12.8859f, 9.589f)
                lineTo(12.8859f, 9.589f)
                curveTo(12.88510f, 9.58810f, 12.88420f, 9.58810f, 12.88420f, 9.58720f)
                curveTo(12.88420f, 9.58810f, 12.88510f, 9.58810f, 12.88590f, 9.5890f)
                moveTo(8.6846f, 25.4709f)
                curveTo(7.68230f, 24.89450f, 6.94720f, 23.86080f, 6.72420f, 22.71340f)
                curveTo(6.50110f, 21.56690f, 6.79530f, 20.3270f, 7.5070f, 19.40790f)
                curveTo(8.2360f, 18.46580f, 8.91290f, 17.86480f, 10.22420f, 17.49910f)
                curveTo(16.88330f, 21.07340f, 23.36790f, 26.84830f, 27.45810f, 33.15710f)
                curveTo(27.45810f, 33.15710f, 9.11340f, 25.71760f, 8.68460f, 25.47090f)
            }
            path(
                fill = SolidColor(Color(0xFF607B8B)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(11.1272f, 6.5483f)
                lineTo(16.4585f, 6.5404f)
                lineTo(20.9436f, 6.5404f)
                curveTo(21.27770f, 4.64660f, 20.60420f, 2.19050f, 19.09240f, 1.03610f)
                curveTo(17.70120f, -0.02670f, 15.75990f, -0.29810f, 14.13780f, 0.34340f)
                curveTo(12.25280f, 1.0890f, 10.77490f, 2.77830f, 9.2570f, 4.15920f)
                lineTo(11.1272f, 6.5483f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(14.625f, 47.9987f)
                lineToRelative(3.6285f, 0f)
                lineToRelative(0f, -28.8737f)
                lineToRelative(-3.6285f, 0f)
                close()
            }
        }.build()
        return _Heron!!
    }

private var _Heron: ImageVector? = null
