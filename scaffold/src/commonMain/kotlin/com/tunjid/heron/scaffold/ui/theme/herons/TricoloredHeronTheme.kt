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
// Redesigned palette: secondary swapped from invented purple to rusty rufous (the real
// neck stripe); primary stays slate-blue for the back; tertiary teal stays for the
// marsh habitat. See design_handoff_heron_themes/README.md.
private val TricoloredHeronPrimaryLight = Color(0xFF1E5A82)
private val TricoloredHeronOnPrimaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronPrimaryContainerLight = Color(0xFFCBE3F7)
private val TricoloredHeronOnPrimaryContainerLight = Color(0xFF001C2E)
private val TricoloredHeronSecondaryLight = Color(0xFF8E4A36)
private val TricoloredHeronOnSecondaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronSecondaryContainerLight = Color(0xFFFFDBCE)
private val TricoloredHeronOnSecondaryContainerLight = Color(0xFF35100A)
private val TricoloredHeronTertiaryLight = Color(0xFF006874)
private val TricoloredHeronOnTertiaryLight = Color(0xFFFFFFFF)
private val TricoloredHeronTertiaryContainerLight = Color(0xFF97F0FF)
private val TricoloredHeronOnTertiaryContainerLight = Color(0xFF001F24)

private val TricoloredHeronPrimaryDark = Color(0xFF9BCAEB)
private val TricoloredHeronOnPrimaryDark = Color(0xFF052E47)
private val TricoloredHeronPrimaryContainerDark = Color(0xFF24465E)
private val TricoloredHeronOnPrimaryContainerDark = Color(0xFFCBE3F7)
private val TricoloredHeronSecondaryDark = Color(0xFFFFB59B)
private val TricoloredHeronOnSecondaryDark = Color(0xFF551C07)
private val TricoloredHeronSecondaryContainerDark = Color(0xFF73341F)
private val TricoloredHeronOnSecondaryContainerDark = Color(0xFFFFDBCE)
private val TricoloredHeronTertiaryDark = Color(0xFF4FD8EB)
private val TricoloredHeronOnTertiaryDark = Color(0xFF00363D)
private val TricoloredHeronTertiaryContainerDark = Color(0xFF004F58)
private val TricoloredHeronOnTertiaryContainerDark = Color(0xFF97F0FF)

val TricoloredHeronLightScheme = lightColorScheme(
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
    surface = Color(0xFFFBFDFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFD8E3EB),
    onSurfaceVariant = Color(0xFF404B53),
    outline = Color(0xFF707B83),
    outlineVariant = Color(0xFFB9C4CC),
    surfaceContainerLow = Color(0xFFECF2F7),
    surfaceContainer = Color(0xFFE2EAF1),
    surfaceContainerHigh = Color(0xFFD7E1EA),
    surfaceContainerHighest = Color(0xFFCCD7E2),
)

val TricoloredHeronDarkScheme = darkColorScheme(
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
    surface = Color(0xFF0D1A25),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF3A4A55),
    onSurfaceVariant = Color(0xFFBBC7D3),
    outline = Color(0xFF85919D),
    outlineVariant = Color(0xFF3A4A55),
    surfaceContainerLowest = Color(0xFF060E16),
    surfaceContainerLow = Color(0xFF121D28),
    surfaceContainer = Color(0xFF16222E),
    surfaceContainerHigh = Color(0xFF1F2C38),
    surfaceContainerHighest = Color(0xFF293643),
)
