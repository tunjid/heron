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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


@Composable
internal inline fun <T> rememberUpdatedStateIf(
    value: T,
    predicate: (T) -> Boolean,
): State<T> = remember {
    mutableStateOf(value)
}.also { if (predicate(value)) it.value = value }

fun viewModelCoroutineScope() = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate
)

internal val BottomNavSharedElementZIndex = 2f
internal val FabSharedElementZIndex = 4f
