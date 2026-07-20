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

package com.tunjid.heron.ui.stateproduction

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tunjid.treenav.strings.Route
import kotlin.reflect.KClass

class ViewModelBackedStateHolderInitializer(
    private val routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
    private val sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
) : StateHolderInitializer {

    override fun createRouteStateHolder(
        type: KClass<out RouteStateHolder>,
        route: Route,
    ): RouteStateHolder {
        val initializer = routeStateHolderInitializers[type]
            ?: throw IllegalStateException(
                """
                    No RouteStateHolderInitializer registered for ${type.simpleName}.
                    Ensure the feature's Bindings contributes one keyed @ClassKey(${type.simpleName}::class).
                """.trimIndent(),
            )
        return initializer.invoke(viewModelCoroutineScope(), route)
    }

    override fun createSheetStateHolder(
        type: KClass<out SheetStateHolder>,
    ): SheetStateHolder {
        val initializer = sheetStateHolderInitializers[type]
            ?: throw IllegalStateException(
                """
                    No SheetStateHolderInitializer registered for ${type.simpleName}.
                    Ensure it is contributed in SheetBindings keyed @ClassKey(${type.simpleName}::class).
                """.trimIndent(),
            )
        return initializer.invoke(viewModelCoroutineScope())
    }
}

@Stable
internal class StateHolderViewModelFactory(
    private val type: KClass<*>,
    private val create: () -> Any,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(
        modelClass: KClass<VM>,
        extras: CreationExtras,
    ): VM {
        val holder = create()
        val viewModel = holder as? ViewModel
            ?: throw IllegalStateException(
                """
                    The initializer for ${type.simpleName} produced ${holder::class.simpleName}, which is
                    not a ViewModel. ViewModelBackedStateHolderInitializer requires ViewModel-backed state
                    holders.
                """.trimIndent(),
            )
        return viewModel as VM
    }
}
