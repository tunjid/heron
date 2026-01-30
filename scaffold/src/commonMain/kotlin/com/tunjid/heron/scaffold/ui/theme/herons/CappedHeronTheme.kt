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

// Capped Heron
private val CappedHeronPrimaryLight = Color(0xFF3D6298)
private val CappedHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val CappedHeronPrimaryContainerLight = Color(0xFFD5E3FF)
private val CappedHeronOnPrimaryContainerLight = Color(0xFF001C3B)
private val CappedHeronSecondaryLight = Color(0xFF5E5E5E)
private val CappedHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val CappedHeronSecondaryContainerLight = Color(0xFFE3E3E3)
private val CappedHeronOnSecondaryContainerLight = Color(0xFF1B1B1B)
private val CappedHeronTertiaryLight = Color(0xFF6C5E00)
private val CappedHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val CappedHeronTertiaryContainerLight = Color(0xFFFFE351)
private val CappedHeronOnTertiaryContainerLight = Color(0xFF211B00)

private val CappedHeronPrimaryDark = Color(0xFFA8C8FF)
private val CappedHeronOnPrimaryDark = Color(0xFF003061)
private val CappedHeronPrimaryContainerDark = Color(0xFF22497E)
private val CappedHeronOnPrimaryContainerDark = Color(0xFFD5E3FF)
private val CappedHeronSecondaryDark = Color(0xFFC6C6C6)
private val CappedHeronOnSecondaryDark = Color(0xFF303030)
private val CappedHeronSecondaryContainerDark = Color(0xFF474747)
private val CappedHeronOnSecondaryContainerDark = Color(0xFFE3E3E3)
private val CappedHeronTertiaryDark = Color(0xFFE2C646)
private val CappedHeronOnTertiaryDark = Color(0xFF383000)
private val CappedHeronTertiaryContainerDark = Color(0xFF514700)
private val CappedHeronOnTertiaryContainerDark = Color(0xFFFFE351)

val CappedHeronLightScheme = lightColorScheme(
    primary = CappedHeronPrimaryLight,
    onPrimary = CappedHeronOnPrimaryLight,
    primaryContainer = CappedHeronPrimaryContainerLight,
    onPrimaryContainer = CappedHeronOnPrimaryContainerLight,
    secondary = CappedHeronSecondaryLight,
    onSecondary = CappedHeronOnSecondaryLight,
    secondaryContainer = CappedHeronSecondaryContainerLight,
    onSecondaryContainer = CappedHeronOnSecondaryContainerLight,
    tertiary = CappedHeronTertiaryLight,
    onTertiary = CappedHeronOnTertiaryLight,
    tertiaryContainer = CappedHeronTertiaryContainerLight,
    onTertiaryContainer = CappedHeronOnTertiaryContainerLight,
    surface = Color(0xFFF8FAFF),
    onSurface = Color(0xFF191C20),
)

val CappedHeronDarkScheme = darkColorScheme(
    primary = CappedHeronPrimaryDark,
    onPrimary = CappedHeronOnPrimaryDark,
    primaryContainer = CappedHeronPrimaryContainerDark,
    onPrimaryContainer = CappedHeronOnPrimaryContainerDark,
    secondary = CappedHeronSecondaryDark,
    onSecondary = CappedHeronOnSecondaryDark,
    secondaryContainer = CappedHeronSecondaryContainerDark,
    onSecondaryContainer = CappedHeronOnSecondaryContainerDark,
    tertiary = CappedHeronTertiaryDark,
    onTertiary = CappedHeronOnTertiaryDark,
    tertiaryContainer = CappedHeronTertiaryContainerDark,
    onTertiaryContainer = CappedHeronOnTertiaryContainerDark,
    surface = Color(0xFF001C3B),
    onSurface = Color(0xFFE1E2E8),
)
