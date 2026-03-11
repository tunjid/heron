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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Eurosky: ImageVector
    get() {
        if (_Eurosky != null) {
            return _Eurosky!!
        }
        _Eurosky = ImageVector.Builder(
            name = "Eurosky",
            defaultWidth = 500.dp,
            defaultHeight = 500.dp,
            viewportWidth = 500f,
            viewportHeight = 500f,
        ).apply {
            // Lines from center to outer nodes
            path(
                stroke = SolidColor(Color(0xFF2660A4)),
                strokeLineWidth = 18f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(240f, 260f)
                lineTo(245f, 55f)
            }
            path(
                stroke = SolidColor(Color(0xFF2660A4)),
                strokeLineWidth = 18f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(240f, 260f)
                lineTo(420f, 150f)
            }
            path(
                stroke = SolidColor(Color(0xFF2660A4)),
                strokeLineWidth = 18f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(240f, 260f)
                lineTo(410f, 400f)
            }
            path(
                stroke = SolidColor(Color(0xFF2660A4)),
                strokeLineWidth = 18f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(240f, 260f)
                lineTo(135f, 430f)
            }
            path(
                stroke = SolidColor(Color(0xFF2660A4)),
                strokeLineWidth = 18f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(240f, 260f)
                lineTo(65f, 245f)
            }
            // Outer nodes
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(275f, 55f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 215f, 55f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 275f, 55f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(450f, 150f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 390f, 150f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 450f, 150f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(440f, 400f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 380f, 400f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 440f, 400f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(165f, 430f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 105f, 430f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 165f, 430f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(95f, 245f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 35f, 245f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 95f, 245f)
                close()
            }
            // Center node
            path(fill = SolidColor(Color(0xFFFFD020))) {
                moveTo(270f, 260f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 210f, 260f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = true, isPositiveArc = true, 270f, 260f)
                close()
            }
        }.build()

        return _Eurosky!!
    }

@Suppress("ObjectPropertyName")
private var _Eurosky: ImageVector? = null
