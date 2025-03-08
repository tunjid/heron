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

package com.tunjid.heron.scaffold.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.Roboto_Black
import heron.scaffold.generated.resources.Roboto_BlackItalic
import heron.scaffold.generated.resources.Roboto_Bold
import heron.scaffold.generated.resources.Roboto_BoldItalic
import heron.scaffold.generated.resources.Roboto_ExtraBold
import heron.scaffold.generated.resources.Roboto_ExtraBoldItalic
import heron.scaffold.generated.resources.Roboto_Italic
import heron.scaffold.generated.resources.Roboto_Light
import heron.scaffold.generated.resources.Roboto_LightItalic
import heron.scaffold.generated.resources.Roboto_Medium
import heron.scaffold.generated.resources.Roboto_MediumItalic
import heron.scaffold.generated.resources.Roboto_Regular
import heron.scaffold.generated.resources.Roboto_SemiBold
import heron.scaffold.generated.resources.Roboto_SemiBoldItalic
import org.jetbrains.compose.resources.Font

fun appTypography(fontFamily: FontFamily): Typography {
    val base = Typography()

    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = fontFamily,
            fontSize = base.bodyLarge.fontSize * 0.95f,
        ),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun appFont(): FontFamily {
    return FontFamily(
        Font(
            resource = Res.font.Roboto_Black,
            weight = FontWeight.Black,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_BlackItalic,
            weight = FontWeight.Black,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_Bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_BoldItalic,
            weight = FontWeight.Bold,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_ExtraBold,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_ExtraBoldItalic,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_Light,
            weight = FontWeight.Light,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_LightItalic,
            weight = FontWeight.Light,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_Medium,
            weight = FontWeight.Medium,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_MediumItalic,
            weight = FontWeight.Medium,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_Regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_Italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.Roboto_SemiBold,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.Roboto_SemiBoldItalic,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic,
        ),
    )
}