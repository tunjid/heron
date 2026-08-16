package com.tunjid.heron.ui.scaffold.ui

import androidx.compose.runtime.Stable
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffoldState.DismissBehavior
import com.tunjid.heron.ui.scaffold.scaffold.PaneAnchor
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

@Stable
@Snapshottable
interface UiState {
    @SnapshotSpec
    data class Immutable(
        val showNavigation: Boolean = false,
        val dismissBehavior: DismissBehavior = DismissBehavior.None,
        val currentPaneAnchor: PaneAnchor = PaneAnchor.Half,
    ) : UiState
}

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
}
