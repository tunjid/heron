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

package com.tunjid.heron.scaffold.ui.theme.herons

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Green Heron
private val GreenHeronPrimaryLight = Color(0xFF186C32)
private val GreenHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val GreenHeronPrimaryContainerLight = Color(0xFFA3F6AC)
private val GreenHeronOnPrimaryContainerLight = Color(0xFF002109)
private val GreenHeronSecondaryLight = Color(0xFF9A4522)
private val GreenHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val GreenHeronSecondaryContainerLight = Color(0xFFFFDBCE)
private val GreenHeronOnSecondaryContainerLight = Color(0xFF390C00)
private val GreenHeronTertiaryLight = Color(0xFF006C4C)
private val GreenHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val GreenHeronTertiaryContainerLight = Color(0xFF89F8C7)
private val GreenHeronOnTertiaryContainerLight = Color(0xFF002114)

private val GreenHeronPrimaryDark = Color(0xFF88D992)
private val GreenHeronOnPrimaryDark = Color(0xFF003915)
private val GreenHeronPrimaryContainerDark = Color(0xFF005323)
private val GreenHeronOnPrimaryContainerDark = Color(0xFFA3F6AC)
private val GreenHeronSecondaryDark = Color(0xFFFFB59B)
private val GreenHeronOnSecondaryDark = Color(0xFF5D1900)
private val GreenHeronSecondaryContainerDark = Color(0xFF7C2E11)
private val GreenHeronOnSecondaryContainerDark = Color(0xFFFFDBCE)
private val GreenHeronTertiaryDark = Color(0xFF6BDAAC)
private val GreenHeronOnTertiaryDark = Color(0xFF003825)
private val GreenHeronTertiaryContainerDark = Color(0xFF005138)
private val GreenHeronOnTertiaryContainerDark = Color(0xFF89F8C7)

val GreenHeronLightScheme =
    lightColorScheme(
        primary = GreenHeronPrimaryLight,
        onPrimary = GreenHeronOnPrimaryLight,
        primaryContainer = GreenHeronPrimaryContainerLight,
        onPrimaryContainer = GreenHeronOnPrimaryContainerLight,
        secondary = GreenHeronSecondaryLight,
        onSecondary = GreenHeronOnSecondaryLight,
        secondaryContainer = GreenHeronSecondaryContainerLight,
        onSecondaryContainer = GreenHeronOnSecondaryContainerLight,
        tertiary = GreenHeronTertiaryLight,
        onTertiary = GreenHeronOnTertiaryLight,
        tertiaryContainer = GreenHeronTertiaryContainerLight,
        onTertiaryContainer = GreenHeronOnTertiaryContainerLight,
        surface = Color(0xFFF0FDF4),
        onSurface = Color(0xFF191C20),
    )

val GreenHeronDarkScheme =
    darkColorScheme(
        primary = GreenHeronPrimaryDark,
        onPrimary = GreenHeronOnPrimaryDark,
        primaryContainer = GreenHeronPrimaryContainerDark,
        onPrimaryContainer = GreenHeronOnPrimaryContainerDark,
        secondary = GreenHeronSecondaryDark,
        onSecondary = GreenHeronOnSecondaryDark,
        secondaryContainer = GreenHeronSecondaryContainerDark,
        onSecondaryContainer = GreenHeronOnSecondaryContainerDark,
        tertiary = GreenHeronTertiaryDark,
        onTertiary = GreenHeronOnTertiaryDark,
        tertiaryContainer = GreenHeronTertiaryContainerDark,
        onTertiaryContainer = GreenHeronOnTertiaryContainerDark,
        surface = Color(0xFF002109),
        onSurface = Color(0xFFE1E2E8),
    )
