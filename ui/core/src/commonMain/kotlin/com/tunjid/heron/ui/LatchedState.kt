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

package com.tunjid.heron.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A state object that "latches" onto a written value.
 *
 * Once [latch] is called, this state will reflect the written value and ignore
 * subsequent writes until the upstream value provided to [rememberLatchedState]
 * changes to match or supersede the latched value.
 */
@Stable
interface LatchedState<T> : State<T> {
    /**
     * Returns true if the local state matches the upstream source of truth.
     * Returns false if a value has been latched that hasn't been reflected upstream yet.
     */
    val isCurrent: Boolean

    /**
     * Eagerly updates the local state to the new value.
     *
     * This operation is "one-way" per sync cycle. Once a value is latched,
     * further calls to [latch] are ignored until the upstream source catches up.
     */
    fun latch(newValue: T)
}

@Composable
fun <T> rememberLatchedState(actualValue: T): LatchedState<T> {
    val state = remember {
        SingleWriteLatchedState(actualValue)
    }

    // If caught up or diverged, update
    remember(actualValue) { state.reset(actualValue) }

    return state
}

/**
 * Private implementation to encapsulate the locking logic.
 */
private class SingleWriteLatchedState<T>(
    initialValue: T,
) : LatchedState<T> {

    private val state = mutableStateOf(initialValue)
    var upstreamValue by mutableStateOf(initialValue)

    override val value: T
        get() = state.value

    override val isCurrent: Boolean
        get() = value == upstreamValue

    override fun latch(newValue: T) {
        // Already latched
        if (!isCurrent) return
        state.value = newValue
    }

    fun reset(actualValue: T) {
        upstreamValue = actualValue
        state.value = actualValue
    }
}
