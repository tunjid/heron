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

// Agami Heron
private val AgamiHeronPrimaryLight = Color(0xFF006A60)
private val AgamiHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val AgamiHeronPrimaryContainerLight = Color(0xFF73F8E9)
private val AgamiHeronOnPrimaryContainerLight = Color(0xFF00201C)
private val AgamiHeronSecondaryLight = Color(0xFF904A42)
private val AgamiHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val AgamiHeronSecondaryContainerLight = Color(0xFFFFDAD5)
private val AgamiHeronOnSecondaryContainerLight = Color(0xFF3B0906)
private val AgamiHeronTertiaryLight = Color(0xFF00658F)
private val AgamiHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val AgamiHeronTertiaryContainerLight = Color(0xFFC8E6FF)
private val AgamiHeronOnTertiaryContainerLight = Color(0xFF001E2E)

private val AgamiHeronPrimaryDark = Color(0xFF52DBC9)
private val AgamiHeronOnPrimaryDark = Color(0xFF003732)
private val AgamiHeronPrimaryContainerDark = Color(0xFF005048)
private val AgamiHeronOnPrimaryContainerDark = Color(0xFF73F8E9)
private val AgamiHeronSecondaryDark = Color(0xFFFFB4A9)
private val AgamiHeronOnSecondaryDark = Color(0xFF561E18)
private val AgamiHeronSecondaryContainerDark = Color(0xFF73332C)
private val AgamiHeronOnSecondaryContainerDark = Color(0xFFFFDAD5)
private val AgamiHeronTertiaryDark = Color(0xFF86CFFF)
private val AgamiHeronOnTertiaryDark = Color(0xFF00344C)
private val AgamiHeronTertiaryContainerDark = Color(0xFF004C6D)
private val AgamiHeronOnTertiaryContainerDark = Color(0xFFC8E6FF)

val AgamiHeronLightScheme = lightColorScheme(
    primary = AgamiHeronPrimaryLight,
    onPrimary = AgamiHeronOnPrimaryLight,
    primaryContainer = AgamiHeronPrimaryContainerLight,
    onPrimaryContainer = AgamiHeronOnPrimaryContainerLight,
    secondary = AgamiHeronSecondaryLight,
    onSecondary = AgamiHeronOnSecondaryLight,
    secondaryContainer = AgamiHeronSecondaryContainerLight,
    onSecondaryContainer = AgamiHeronOnSecondaryContainerLight,
    tertiary = AgamiHeronTertiaryLight,
    onTertiary = AgamiHeronOnTertiaryLight,
    tertiaryContainer = AgamiHeronTertiaryContainerLight,
    onTertiaryContainer = AgamiHeronOnTertiaryContainerLight,
    surface = Color(0xFFF0FDFB),
    onSurface = Color(0xFF191C20),
)

val AgamiHeronDarkScheme = darkColorScheme(
    primary = AgamiHeronPrimaryDark,
    onPrimary = AgamiHeronOnPrimaryDark,
    primaryContainer = AgamiHeronPrimaryContainerDark,
    onPrimaryContainer = AgamiHeronOnPrimaryContainerDark,
    secondary = AgamiHeronSecondaryDark,
    onSecondary = AgamiHeronOnSecondaryDark,
    secondaryContainer = AgamiHeronSecondaryContainerDark,
    onSecondaryContainer = AgamiHeronOnSecondaryContainerDark,
    tertiary = AgamiHeronTertiaryDark,
    onTertiary = AgamiHeronOnTertiaryDark,
    tertiaryContainer = AgamiHeronTertiaryContainerDark,
    onTertiaryContainer = AgamiHeronOnTertiaryContainerDark,
    surface = Color(0xFF00201C),
    onSurface = Color(0xFFE1E2E8),
)
