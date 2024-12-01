package com.tunjid.heron

import androidx.compose.runtime.Stable
import com.tunjid.heron.navigation.NavigationStateHolder
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