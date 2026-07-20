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

package com.tunjid.heron.ui.scaffold.ui.theme.herons

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Black Heron
private val BlackHeronPrimaryLight = Color(0xFF1A1A1A)
private val BlackHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val BlackHeronPrimaryContainerLight = Color(0xFFE0E0E0)
private val BlackHeronOnPrimaryContainerLight = Color(0xFF000000)
private val BlackHeronSecondaryLight = Color(0xFF3D3D3D)
private val BlackHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val BlackHeronSecondaryContainerLight = Color(0xFFEBEBEB)
private val BlackHeronOnSecondaryContainerLight = Color(0xFF1A1A1A)
private val BlackHeronTertiaryLight = Color(0xFF5C5C5C)
private val BlackHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val BlackHeronTertiaryContainerLight = Color(0xFFF0F0F0)
private val BlackHeronOnTertiaryContainerLight = Color(0xFF2E2E2E)

private val BlackHeronPrimaryDark = Color(0xFFE6E6E6)
private val BlackHeronOnPrimaryDark = Color(0xFF000000)
private val BlackHeronPrimaryContainerDark = Color(0xFF262626)
private val BlackHeronOnPrimaryContainerDark = Color(0xFFFFFFFF)
private val BlackHeronSecondaryDark = Color(0xFFC2C2C2)
private val BlackHeronOnSecondaryDark = Color(0xFF000000)
private val BlackHeronSecondaryContainerDark = Color(0xFF1C1C1C)
private val BlackHeronOnSecondaryContainerDark = Color(0xFFE6E6E6)
private val BlackHeronTertiaryDark = Color(0xFF9E9E9E)
private val BlackHeronOnTertiaryDark = Color(0xFF000000)
private val BlackHeronTertiaryContainerDark = Color(0xFF161616)
private val BlackHeronOnTertiaryContainerDark = Color(0xFFD1D1D1)

val BlackHeronLightScheme = lightColorScheme(
    primary = BlackHeronPrimaryLight,
    onPrimary = BlackHeronOnPrimaryLight,
    primaryContainer = BlackHeronPrimaryContainerLight,
    onPrimaryContainer = BlackHeronOnPrimaryContainerLight,
    secondary = BlackHeronSecondaryLight,
    onSecondary = BlackHeronOnSecondaryLight,
    secondaryContainer = BlackHeronSecondaryContainerLight,
    onSecondaryContainer = BlackHeronOnSecondaryContainerLight,
    tertiary = BlackHeronTertiaryLight,
    onTertiary = BlackHeronOnTertiaryLight,
    tertiaryContainer = BlackHeronTertiaryContainerLight,
    onTertiaryContainer = BlackHeronOnTertiaryContainerLight,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE8E8E8),
)

val BlackHeronDarkScheme = darkColorScheme(
    primary = BlackHeronPrimaryDark,
    onPrimary = BlackHeronOnPrimaryDark,
    primaryContainer = BlackHeronPrimaryContainerDark,
    onPrimaryContainer = BlackHeronOnPrimaryContainerDark,
    secondary = BlackHeronSecondaryDark,
    onSecondary = BlackHeronOnSecondaryDark,
    secondaryContainer = BlackHeronSecondaryContainerDark,
    onSecondaryContainer = BlackHeronOnSecondaryContainerDark,
    tertiary = BlackHeronTertiaryDark,
    onTertiary = BlackHeronOnTertiaryDark,
    tertiaryContainer = BlackHeronTertiaryContainerDark,
    onTertiaryContainer = BlackHeronOnTertiaryContainerDark,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
)
