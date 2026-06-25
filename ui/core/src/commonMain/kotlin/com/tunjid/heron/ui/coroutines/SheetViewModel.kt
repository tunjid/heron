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

package com.tunjid.heron.ui.coroutines

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for the [ViewModel]s that back app level bottom sheets. Their lifecycle is owned by
 * the [scope] passed in (typically a [viewModelCoroutineScope]), so they can be created on demand
 * from the app's dependency graph via a [SheetViewModelInitializer].
 */
abstract class SheetViewModel(
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope)

/**
 * Factory for a [SheetViewModel] given its [CoroutineScope]. Sheet modules contribute one of these
 * per sheet into the app graph's `Map<KClass<out SheetViewModel>, SheetViewModelInitializer>`,
 * mirroring how feature modules contribute their navigation entries.
 */
fun interface SheetViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): SheetViewModel
}
