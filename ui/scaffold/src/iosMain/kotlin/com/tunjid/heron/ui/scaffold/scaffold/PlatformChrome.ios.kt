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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import platform.UIKit.UIColor
import platform.UIKit.UIDevice

internal fun isIos26OrLater(): Boolean =
    (UIDevice.currentDevice.systemVersion.substringBefore(".").toIntOrNull() ?: 0) >= 26

/**
 * See the `generateIosIconAssets` task in `:ui:icons`.
 */
internal fun ImageVector.heronIconAssetName(): String {
    val leaf = name.substringAfterLast('.')
    return buildString {
        leaf.forEachIndexed { index, character ->
            if (character.isUpperCase()) {
                if (index != 0) append('_')
                append(character.lowercaseChar())
            } else {
                append(character)
            }
        }
    }
}

internal fun Color.toUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
