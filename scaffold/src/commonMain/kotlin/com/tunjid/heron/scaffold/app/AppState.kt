package com.tunjid.heron.scaffold.app

import androidx.compose.runtime.Stable
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import me.tatarka.inject.annotations.Inject

@Inject
@Stable
class AppState(
    routeConfigurationMap: Map<String, PaneStrategy<ThreePane, Route>>,
    navigationStateHolder: NavigationStateHolder,
)