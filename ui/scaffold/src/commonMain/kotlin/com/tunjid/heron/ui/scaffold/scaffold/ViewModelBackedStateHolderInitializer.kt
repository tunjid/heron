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

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.StateHolderInitializer
import com.tunjid.heron.ui.stateproduction.viewModelCoroutineScope
import com.tunjid.treenav.strings.Route
import kotlin.reflect.KClass

/**
 * The real app's [StateHolderInitializer]. It resolves the per-screen / per-sheet factory from the
 * DI multibinding maps (keyed by the state holder interface) and retains the produced holder in an
 * AndroidX `ViewModel` store. The AndroidX `ViewModel` dependency lives here and nowhere on the
 * route/sheet facing surface; swapping retention (e.g. for `retain`) only touches this class.
 */
class ViewModelBackedStateHolderInitializer(
    private val routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
    private val sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
) : StateHolderInitializer {

    @Composable
    override fun <T : RouteStateHolder> retainRouteStateHolder(
        type: KClass<T>,
        route: Route,
    ): T {
        val initializer = routeStateHolderInitializers[type]
            ?: throw IllegalStateException(
                "No RouteStateHolderInitializer registered for ${type.simpleName}. " +
                    "Ensure the feature's Bindings contributes one keyed @ClassKey(${type.simpleName}::class).",
            )
        @Suppress("UNCHECKED_CAST")
        return viewModel(
            modelClass = type as KClass<ViewModel>,
            factory = viewModelBackedStateHolderFactory(type) {
                initializer.invoke(viewModelCoroutineScope(), route)
            },
        ) as T
    }

    @Composable
    override fun <T : SheetStateHolder> retainSheetStateHolder(
        type: KClass<T>,
    ): T {
        val initializer = sheetStateHolderInitializers[type]
            ?: throw IllegalStateException(
                "No SheetStateHolderInitializer registered for ${type.simpleName}. " +
                    "Ensure it is contributed in SheetBindings keyed @ClassKey(${type.simpleName}::class).",
            )
        @Suppress("UNCHECKED_CAST")
        return viewModel(
            modelClass = type as KClass<ViewModel>,
            // Sheets get their own store so they are retained for the lifetime of the sheet's
            // composition rather than the navigation entry, matching the previous behaviour.
            viewModelStoreOwner = rememberViewModelStoreOwner(
                provider = rememberViewModelStoreProvider(),
            ),
            factory = viewModelBackedStateHolderFactory(type) {
                initializer.invoke(viewModelCoroutineScope())
            },
        ) as T
    }
}

/**
 * Builds a [ViewModelProvider.Factory] that produces a state holder via [create] and asserts the
 * result is an AndroidX `ViewModel` so it can be retained, throwing a detailed error otherwise.
 */
private fun viewModelBackedStateHolderFactory(
    type: KClass<*>,
    create: () -> Any,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(
        modelClass: KClass<VM>,
        extras: CreationExtras,
    ): VM {
        val holder = create()
        val viewModel = holder as? ViewModel
            ?: throw IllegalStateException(
                "The initializer for ${type.simpleName} produced ${holder::class.simpleName}, " +
                    "which is not a ViewModel. ViewModelBackedStateHolderInitializer requires " +
                    "ViewModel-backed state holders.",
            )
        return viewModel as VM
    }
}
