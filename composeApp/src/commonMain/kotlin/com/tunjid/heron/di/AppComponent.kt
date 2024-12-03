/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.di

import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.signin.di.SignInScreenHolderComponent
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
    @Component val signInComponent: SignInScreenHolderComponent,
) {

    abstract val routeConfigurationMap: Map<String, PaneStrategy<ThreePane, Route>>

    @Provides
    fun appState(
        navigationStateHolder: NavigationStateHolder,
        savedStateRepository: SavedStateRepository,
    ): AppState = AppState(
        routeConfigurationMap = routeConfigurationMap,
        navigationStateHolder = navigationStateHolder,
        savedStateRepository = savedStateRepository,
    )

    abstract val appState: AppState
}
