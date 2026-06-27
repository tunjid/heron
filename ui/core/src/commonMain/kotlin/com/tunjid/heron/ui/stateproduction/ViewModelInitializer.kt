package com.tunjid.heron.ui.stateproduction

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

@Stable
interface ViewModelInitializer {
    fun sheetViewModelInitializer(
        modelClass: KClass<out SheetViewModel>,
    ): SheetViewModelInitializer

    fun routeViewModelInitializer(
        modelClass: KClass<out RouteViewModel>,
    ): RouteViewModelInitializer
}
