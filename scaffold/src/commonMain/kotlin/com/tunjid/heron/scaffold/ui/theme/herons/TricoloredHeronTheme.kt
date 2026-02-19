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

// Tricolored Heron
private val TricoloredHeronPrimaryLight = Color(0xFF00639A)
private val TricoloredHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronPrimaryContainerLight = Color(0xFFCEE5FF)
private val TricoloredHeronOnPrimaryContainerLight = Color(0xFF001D32)
private val TricoloredHeronSecondaryLight = Color(0xFF6F4DA0)
private val TricoloredHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronSecondaryContainerLight = Color(0xFFEDDCFF)
private val TricoloredHeronOnSecondaryContainerLight = Color(0xFF290055)
private val TricoloredHeronTertiaryLight = Color(0xFF006874)
private val TricoloredHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronTertiaryContainerLight = Color(0xFF97F0FF)
private val TricoloredHeronOnTertiaryContainerLight = Color(0xFF001F24)

private val TricoloredHeronPrimaryDark = Color(0xFF95CCFF)
private val TricoloredHeronOnPrimaryDark = Color(0xFF003352)
private val TricoloredHeronPrimaryContainerDark = Color(0xFF004A75)
private val TricoloredHeronOnPrimaryContainerDark = Color(0xFFCEE5FF)
private val TricoloredHeronSecondaryDark = Color(0xFFDAB9FF)
private val TricoloredHeronOnSecondaryDark = Color(0xFF3F1B6E)
private val TricoloredHeronSecondaryContainerDark = Color(0xFF573486)
private val TricoloredHeronOnSecondaryContainerDark = Color(0xFFEDDCFF)
private val TricoloredHeronTertiaryDark = Color(0xFF4FD8EB)
private val TricoloredHeronOnTertiaryDark = Color(0xFF00363D)
private val TricoloredHeronTertiaryContainerDark = Color(0xFF004F58)
private val TricoloredHeronOnTertiaryContainerDark = Color(0xFF97F0FF)

val TricoloredHeronLightScheme =
    lightColorScheme(
        primary = TricoloredHeronPrimaryLight,
        onPrimary = TricoloredHeronOnPrimaryLight,
        primaryContainer = TricoloredHeronPrimaryContainerLight,
        onPrimaryContainer = TricoloredHeronOnPrimaryContainerLight,
        secondary = TricoloredHeronSecondaryLight,
        onSecondary = TricoloredHeronOnSecondaryLight,
        secondaryContainer = TricoloredHeronSecondaryContainerLight,
        onSecondaryContainer = TricoloredHeronOnSecondaryContainerLight,
        tertiary = TricoloredHeronTertiaryLight,
        onTertiary = TricoloredHeronOnTertiaryLight,
        tertiaryContainer = TricoloredHeronTertiaryContainerLight,
        onTertiaryContainer = TricoloredHeronOnTertiaryContainerLight,
        surface = Color(0xFFF0F7FF),
        onSurface = Color(0xFF191C20),
    )

val TricoloredHeronDarkScheme =
    darkColorScheme(
        primary = TricoloredHeronPrimaryDark,
        onPrimary = TricoloredHeronOnPrimaryDark,
        primaryContainer = TricoloredHeronPrimaryContainerDark,
        onPrimaryContainer = TricoloredHeronOnPrimaryContainerDark,
        secondary = TricoloredHeronSecondaryDark,
        onSecondary = TricoloredHeronOnSecondaryDark,
        secondaryContainer = TricoloredHeronSecondaryContainerDark,
        onSecondaryContainer = TricoloredHeronOnSecondaryContainerDark,
        tertiary = TricoloredHeronTertiaryDark,
        onTertiary = TricoloredHeronOnTertiaryDark,
        tertiaryContainer = TricoloredHeronTertiaryContainerDark,
        onTertiaryContainer = TricoloredHeronOnTertiaryContainerDark,
        surface = Color(0xFF001D32),
        onSurface = Color(0xFFE1E2E8),
    )
