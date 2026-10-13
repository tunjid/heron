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

package com.tunjid.heron.ui.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.blockClickEvents() =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            waitForUpOrCancellation(PointerEventPass.Initial)?.consume()
        }
    }

inline fun Modifier.ifTrue(
    predicate: Boolean,
    crossinline block: Modifier.() -> Modifier,
) = if (predicate) block() else this

inline fun <T> Modifier.ifNotNull(
    item: T?,
    crossinline block: Modifier.(T) -> Modifier,
) = if (item != null) block(item) else this
