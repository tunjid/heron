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

// Reddish Egret
// Redesigned palette: primary = rusty/cinnamon (matches the name), secondary = slate-gray
// (matches the body), tertiary = dusty pink. See design_handoff_heron_themes/README.md.
private val ReddishEgretPrimaryLight = Color(0xFFA04432)
private val ReddishEgretOnPrimaryLight = Color(0xFFFFFFFF)
private val ReddishEgretPrimaryContainerLight = Color(0xFFFFDAD1)
private val ReddishEgretOnPrimaryContainerLight = Color(0xFF3E0900)
private val ReddishEgretSecondaryLight = Color(0xFF59616B)
private val ReddishEgretOnSecondaryLight = Color(0xFFFFFFFF)
private val ReddishEgretSecondaryContainerLight = Color(0xFFDCE2EC)
private val ReddishEgretOnSecondaryContainerLight = Color(0xFF161C26)
private val ReddishEgretTertiaryLight = Color(0xFF8B4F66)
private val ReddishEgretOnTertiaryLight = Color(0xFFFFFFFF)
private val ReddishEgretTertiaryContainerLight = Color(0xFFFFD8E6)
private val ReddishEgretOnTertiaryContainerLight = Color(0xFF370B21)

private val ReddishEgretPrimaryDark = Color(0xFFFFB5A2)
private val ReddishEgretOnPrimaryDark = Color(0xFF5E1905)
private val ReddishEgretPrimaryContainerDark = Color(0xFF7D2E1A)
private val ReddishEgretOnPrimaryContainerDark = Color(0xFFFFDAD1)
private val ReddishEgretSecondaryDark = Color(0xFFC2CAD4)
private val ReddishEgretOnSecondaryDark = Color(0xFF2B313B)
private val ReddishEgretSecondaryContainerDark = Color(0xFF424852)
private val ReddishEgretOnSecondaryContainerDark = Color(0xFFDCE2EC)
private val ReddishEgretTertiaryDark = Color(0xFFFDB5CA)
private val ReddishEgretOnTertiaryDark = Color(0xFF502037)
private val ReddishEgretTertiaryContainerDark = Color(0xFF6B374D)
private val ReddishEgretOnTertiaryContainerDark = Color(0xFFFFD8E6)

val ReddishEgretLightScheme = lightColorScheme(
    primary = ReddishEgretPrimaryLight,
    onPrimary = ReddishEgretOnPrimaryLight,
    primaryContainer = ReddishEgretPrimaryContainerLight,
    onPrimaryContainer = ReddishEgretOnPrimaryContainerLight,
    secondary = ReddishEgretSecondaryLight,
    onSecondary = ReddishEgretOnSecondaryLight,
    secondaryContainer = ReddishEgretSecondaryContainerLight,
    onSecondaryContainer = ReddishEgretOnSecondaryContainerLight,
    tertiary = ReddishEgretTertiaryLight,
    onTertiary = ReddishEgretOnTertiaryLight,
    tertiaryContainer = ReddishEgretTertiaryContainerLight,
    onTertiaryContainer = ReddishEgretOnTertiaryContainerLight,
    surface = Color(0xFFFBF3F1),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFEEDAD3),
    onSurfaceVariant = Color(0xFF554040),
    outline = Color(0xFF8B7272),
    outlineVariant = Color(0xFFD5BFBE),
    surfaceContainerLowest = Color(0xFFFFFBFA),
    surfaceContainerLow = Color(0xFFF7EEEB),
    surfaceContainer = Color(0xFFF4EAE6),
    surfaceContainerHigh = Color(0xFFEEE3DE),
    surfaceContainerHighest = Color(0xFFE8DBD5),
)

val ReddishEgretDarkScheme = darkColorScheme(
    primary = ReddishEgretPrimaryDark,
    onPrimary = ReddishEgretOnPrimaryDark,
    primaryContainer = ReddishEgretPrimaryContainerDark,
    onPrimaryContainer = ReddishEgretOnPrimaryContainerDark,
    secondary = ReddishEgretSecondaryDark,
    onSecondary = ReddishEgretOnSecondaryDark,
    secondaryContainer = ReddishEgretSecondaryContainerDark,
    onSecondaryContainer = ReddishEgretOnSecondaryContainerDark,
    tertiary = ReddishEgretTertiaryDark,
    onTertiary = ReddishEgretOnTertiaryDark,
    tertiaryContainer = ReddishEgretTertiaryContainerDark,
    onTertiaryContainer = ReddishEgretOnTertiaryContainerDark,
    surface = Color(0xFF1E1311),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF554040),
    onSurfaceVariant = Color(0xFFD8C1C0),
    outline = Color(0xFFA08A8A),
    outlineVariant = Color(0xFF554040),
    surfaceContainerLowest = Color(0xFF130807),
    surfaceContainerLow = Color(0xFF251916),
    surfaceContainer = Color(0xFF2A1D1A),
    surfaceContainerHigh = Color(0xFF352622),
    surfaceContainerHighest = Color(0xFF40302C),
)
