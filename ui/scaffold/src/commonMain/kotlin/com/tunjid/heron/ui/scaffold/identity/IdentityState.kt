package com.tunjid.heron.ui.scaffold.identity

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.SessionSummary
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.ui.text.Memo
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

sealed class IdentityAction(
    val key: String,
) {
    sealed class Switch :
        IdentityAction(
            key = "switch",
        ) {
        data object Cancel : Switch()
        data object Choose : Switch()
        data class Transition(
            val summary: SessionSummary,
        ) : Switch()
    }

    data object ClearFailedWrite : IdentityAction(
        key = "ClearFailedWrite",
    )
}

@Stable
@Snapshottable
interface IdentityState {
    sealed class SwitchStatus {
        sealed class Stable : SwitchStatus() {
            data object Idle : SwitchStatus.Stable()
            data class Error(
                val memo: Memo,
            ) : SwitchStatus.Stable()
        }

        data object Choosing : SwitchStatus()
        data class Switching(
            val session: SessionSummary,
        ) : SwitchStatus()
    }

    @SnapshotSpec
    data class Immutable(
        val isConnected: Boolean = true,
        val signedInProfile: Profile? = null,
        val preferences: Preferences? = null,
        val lastFailedWrite: FailedWrite? = null,
        val switchStatus: SwitchStatus = SwitchStatus.Stable.Idle,
        val pastSessions: List<SessionSummary> = emptyList(),
    ) : IdentityState
}

internal val IdentityState.isSignedIn
    get() = signedInProfile != null

internal val IdentityState.isStable
    get() = switchStatus is IdentityState.SwitchStatus.Stable

internal val IdentityState.prefersCompactBottomNav: Boolean
    get() = preferences?.local?.useCompactNavigation ?: false

internal val IdentityState.prefersAutoHidingBottomNav: Boolean
    get() = preferences?.local?.autoHideBottomNavigation ?: true
