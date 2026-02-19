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

package com.tunjid.heron.gallery.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

@Stable
class PagerStates<Key> {

    private val keysToPagerStates = mutableStateMapOf<Key, PagerState>()

    @Composable
    fun manage(key: Key, initializer: @Composable () -> PagerState): PagerState {
        val pagerState = initializer().also { keysToPagerStates[key] = it }
        DisposableEffect(key) { onDispose { keysToPagerStates.remove(key) } }
        return pagerState
    }

    @Stable operator fun get(key: Key): PagerState? = keysToPagerStates[key]
}
