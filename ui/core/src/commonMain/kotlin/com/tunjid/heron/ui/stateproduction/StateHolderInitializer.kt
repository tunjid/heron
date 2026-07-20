package com.tunjid.heron.ui.stateproduction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.treenav.strings.Route
import kotlin.reflect.KClass

/**
 * Creates route and sheet state holders on demand.
 */
@Stable
interface StateHolderInitializer {
    fun createRouteStateHolder(
        type: KClass<out RouteStateHolder>,
        route: Route,
    ): RouteStateHolder

    fun createSheetStateHolder(
        type: KClass<out SheetStateHolder>,
    ): SheetStateHolder
}

/**
 * Resolves and retains the [RouteStateHolder] of [type] for [route].
 */
@Composable
@Suppress("UNCHECKED_CAST")
fun StateHolderInitializer.retainRouteStateHolder(
    type: KClass<out RouteStateHolder>,
    route: Route,
): RouteStateHolder = when (this) {
    is ViewModelBackedStateHolderInitializer -> viewModel(
        modelClass = type as KClass<ViewModel>,
        factory = remember(type, route) {
            StateHolderViewModelFactory(type) {
                createRouteStateHolder(type, route)
            }
        },
    ) as RouteStateHolder

    else -> remember(type, route) {
        createRouteStateHolder(type, route)
    }
}

/**
 * Resolves and retains the [SheetStateHolder] of [type].
 */
@Composable
@Suppress("UNCHECKED_CAST")
fun StateHolderInitializer.retainSheetStateHolder(
    type: KClass<out SheetStateHolder>,
): SheetStateHolder = when (this) {
    is ViewModelBackedStateHolderInitializer -> viewModel(
        modelClass = type as KClass<ViewModel>,
        viewModelStoreOwner = rememberViewModelStoreOwner(
            provider = rememberViewModelStoreProvider(),
        ),
        factory = remember(type) {
            StateHolderViewModelFactory(type) {
                createSheetStateHolder(type)
            }
        },
    ) as SheetStateHolder

    else -> remember(type) {
        createSheetStateHolder(type)
    }
}
