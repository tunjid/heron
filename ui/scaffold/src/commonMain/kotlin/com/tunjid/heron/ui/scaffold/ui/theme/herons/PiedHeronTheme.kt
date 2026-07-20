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

// Pied Heron
private val PiedHeronPrimaryLight = Color(0xFF5F5E5E)
private val PiedHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val PiedHeronPrimaryContainerLight = Color(0xFFE5E2E1)
private val PiedHeronOnPrimaryContainerLight = Color(0xFF1C1C1C)
private val PiedHeronSecondaryLight = Color(0xFF5D5F5F)
private val PiedHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val PiedHeronSecondaryContainerLight = Color(0xFFE2E4E4)
private val PiedHeronOnSecondaryContainerLight = Color(0xFF1A1C1C)
private val PiedHeronTertiaryLight = Color(0xFF6A5F00)
private val PiedHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val PiedHeronTertiaryContainerLight = Color(0xFFFFE342)
private val PiedHeronOnTertiaryContainerLight = Color(0xFF201C00)

private val PiedHeronPrimaryDark = Color(0xFFC8C6C6)
private val PiedHeronOnPrimaryDark = Color(0xFF313030)
private val PiedHeronPrimaryContainerDark = Color(0xFF474646)
private val PiedHeronOnPrimaryContainerDark = Color(0xFFE5E2E1)
private val PiedHeronSecondaryDark = Color(0xFFC6C7C7)
private val PiedHeronOnSecondaryDark = Color(0xFF2F3131)
private val PiedHeronSecondaryContainerDark = Color(0xFF454748)
private val PiedHeronOnSecondaryContainerDark = Color(0xFFE2E4E4)
private val PiedHeronTertiaryDark = Color(0xFFDEC700)
private val PiedHeronOnTertiaryDark = Color(0xFF373100)
private val PiedHeronTertiaryContainerDark = Color(0xFF504800)
private val PiedHeronOnTertiaryContainerDark = Color(0xFFFFE342)

val PiedHeronLightScheme = lightColorScheme(
    primary = PiedHeronPrimaryLight,
    onPrimary = PiedHeronOnPrimaryLight,
    primaryContainer = PiedHeronPrimaryContainerLight,
    onPrimaryContainer = PiedHeronOnPrimaryContainerLight,
    secondary = PiedHeronSecondaryLight,
    onSecondary = PiedHeronOnSecondaryLight,
    secondaryContainer = PiedHeronSecondaryContainerLight,
    onSecondaryContainer = PiedHeronOnSecondaryContainerLight,
    tertiary = PiedHeronTertiaryLight,
    onTertiary = PiedHeronOnTertiaryLight,
    tertiaryContainer = PiedHeronTertiaryContainerLight,
    onTertiaryContainer = PiedHeronOnTertiaryContainerLight,
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF191C20),
    surfaceContainerLow = Color(0xFFEFEFEF),
    surfaceContainer = Color(0xFFE8E8E8),
    surfaceContainerHigh = Color(0xFFDEDEDE),
    surfaceContainerHighest = Color(0xFFD3D3D3),
)

val PiedHeronDarkScheme = darkColorScheme(
    primary = PiedHeronPrimaryDark,
    onPrimary = PiedHeronOnPrimaryDark,
    primaryContainer = PiedHeronPrimaryContainerDark,
    onPrimaryContainer = PiedHeronOnPrimaryContainerDark,
    secondary = PiedHeronSecondaryDark,
    onSecondary = PiedHeronOnSecondaryDark,
    secondaryContainer = PiedHeronSecondaryContainerDark,
    onSecondaryContainer = PiedHeronOnSecondaryContainerDark,
    tertiary = PiedHeronTertiaryDark,
    onTertiary = PiedHeronOnTertiaryDark,
    tertiaryContainer = PiedHeronTertiaryContainerDark,
    onTertiaryContainer = PiedHeronOnTertiaryContainerDark,
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFE1E2E8),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF242424),
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF3D3D3D),
)
