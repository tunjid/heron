package com.tunjid.heron.ui.scaffold.ui

import androidx.compose.runtime.Stable
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffoldState.DismissBehavior
import com.tunjid.heron.ui.scaffold.scaffold.PaneAnchor
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.strings.Route

@Stable
@Snapshottable
interface UiState {
    @SnapshotSpec
    data class Immutable(
        val showNavigation: Boolean = false,
        val dismissBehavior: DismissBehavior = DismissBehavior.None,
        val currentPaneAnchor: PaneAnchor = PaneAnchor.Half,
        val immersiveRouteIds: Set<String> = emptySet(),
    ) : UiState
}

val UiState.isImmersive: Boolean
    get() = immersiveRouteIds.isNotEmpty()

sealed class UiAction(
    val key: String,
) {
    data class UpdateShowNavigation(
        val showNavigation: Boolean,
    ) : UiAction("UpdateShowNavigation")

    data class UpdateDismissBehavior(
        val dismissBehavior: DismissBehavior,
    ) : UiAction("UpdateDismissBehavior")

    data class UpdatePaneAnchor(
        val paneAnchor: PaneAnchor,
    ) : UiAction("UpdatePaneAnchor")

    sealed class UpdateRouteImmersion : UiAction("UpdateImmersion") {
        data class Immersive(
            val route: Route,
        ) : UpdateRouteImmersion()

        data class Standard(
            val route: Route,
        ) : UpdateRouteImmersion()
    }
}
