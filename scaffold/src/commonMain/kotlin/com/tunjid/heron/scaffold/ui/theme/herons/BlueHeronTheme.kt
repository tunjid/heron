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

// Blue Heron
private val BlueHeronPrimaryLight = Color(0xFF256489)
private val BlueHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val BlueHeronPrimaryContainerLight = Color(0xFFC9E6FF)
private val BlueHeronOnPrimaryContainerLight = Color(0xFF001E2F)
private val BlueHeronSecondaryLight = Color(0xFF535F70)
private val BlueHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val BlueHeronSecondaryContainerLight = Color(0xFFD7E3F8)
private val BlueHeronOnSecondaryContainerLight = Color(0xFF101C2B)
private val BlueHeronTertiaryLight = Color(0xFF006684)
private val BlueHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val BlueHeronTertiaryContainerLight = Color(0xFFBFE9FF)
private val BlueHeronOnTertiaryContainerLight = Color(0xFF001F2A)

private val BlueHeronPrimaryDark = Color(0xFF95CCF8)
private val BlueHeronOnPrimaryDark = Color(0xFF00344E)
private val BlueHeronPrimaryContainerDark = Color(0xFF004B6F)
private val BlueHeronOnPrimaryContainerDark = Color(0xFFC9E6FF)
private val BlueHeronSecondaryDark = Color(0xFFBBC7DB)
private val BlueHeronOnSecondaryDark = Color(0xFF253140)
private val BlueHeronSecondaryContainerDark = Color(0xFF3B4858)
private val BlueHeronOnSecondaryContainerDark = Color(0xFFD7E3F8)
private val BlueHeronTertiaryDark = Color(0xFF68D3F3)
private val BlueHeronOnTertiaryDark = Color(0xFF003546)
private val BlueHeronTertiaryContainerDark = Color(0xFF004D64)
private val BlueHeronOnTertiaryContainerDark = Color(0xFFBFE9FF)

val BlueHeronLightScheme =
    lightColorScheme(
        primary = BlueHeronPrimaryLight,
        onPrimary = BlueHeronOnPrimaryLight,
        primaryContainer = BlueHeronPrimaryContainerLight,
        onPrimaryContainer = BlueHeronOnPrimaryContainerLight,
        secondary = BlueHeronSecondaryLight,
        onSecondary = BlueHeronOnSecondaryLight,
        secondaryContainer = BlueHeronSecondaryContainerLight,
        onSecondaryContainer = BlueHeronOnSecondaryContainerLight,
        tertiary = BlueHeronTertiaryLight,
        onTertiary = BlueHeronOnTertiaryLight,
        tertiaryContainer = BlueHeronTertiaryContainerLight,
        onTertiaryContainer = BlueHeronOnTertiaryContainerLight,
        surface = Color(0xFFF2F8FF),
        onSurface = Color(0xFF191C20),
    )

val BlueHeronDarkScheme =
    darkColorScheme(
        primary = BlueHeronPrimaryDark,
        onPrimary = BlueHeronOnPrimaryDark,
        primaryContainer = BlueHeronPrimaryContainerDark,
        onPrimaryContainer = BlueHeronOnPrimaryContainerDark,
        secondary = BlueHeronSecondaryDark,
        onSecondary = BlueHeronOnSecondaryDark,
        secondaryContainer = BlueHeronSecondaryContainerDark,
        onSecondaryContainer = BlueHeronOnSecondaryContainerDark,
        tertiary = BlueHeronTertiaryDark,
        onTertiary = BlueHeronOnTertiaryDark,
        tertiaryContainer = BlueHeronTertiaryContainerDark,
        onTertiaryContainer = BlueHeronOnTertiaryContainerDark,
        surface = Color(0xFF001E2F),
        onSurface = Color(0xFFE1E2E8),
    )
