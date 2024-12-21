package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Stable
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.requireThreePaneMovableSharedElementScope
import com.tunjid.treenav.strings.Route

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface SharedElementScope :
    SharedTransitionScope, AnimatedVisibilityScope, MovableSharedElementScope

@Stable
private class PanedSharedElementScope(
    paneScope: PaneScope<ThreePane, Route>,
    movableSharedElementScope: MovableSharedElementScope,
) : SharedElementScope,
    AnimatedVisibilityScope by paneScope,
    MovableSharedElementScope by movableSharedElementScope

fun PaneScope<
        ThreePane,
        Route
        >.requirePanedSharedElementScope(): SharedElementScope =
    PanedSharedElementScope(
        paneScope = this,
        movableSharedElementScope = requireThreePaneMovableSharedElementScope()
    )