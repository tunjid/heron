package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

data class MutedAccount(
    val did: ProfileId,
    val handle: ProfileHandle,
)

sealed class MutedAccountAction {
    data object Load : MutedAccountAction()
    data class Add(val did: ProfileId, val handle: ProfileHandle) : MutedAccountAction()
    data class Remove(val did: ProfileId) : MutedAccountAction()
}

data class MutedAccountState(
    val isLoading: Boolean = false,
    val mutedAccounts: List<MutedAccount> = emptyList(),
)

typealias MutedAccountStateHolder =
    ActionStateMutator<MutedAccountAction, StateFlow<MutedAccountState>>

fun CoroutineScope.mutedAccountMutator(): MutedAccountStateHolder =
    actionStateFlowMutator(
        initialState = MutedAccountState(),
        actionTransform = { actions ->
            actions.toMutationStream {
                when (val action = type()) {
                    MutedAccountAction.Load -> {
                        TODO("Implement Load action")
                    }

                    is MutedAccountAction.Add -> {
                        TODO("Implement Add Account action")
                    }

                    is MutedAccountAction.Remove -> {
                        TODO("Implement Remove Account action")
                    }
                }
            }
        },
    )

@Stable
class MutedAccountsSheetState private constructor(
    val mutator: MutedAccountStateHolder,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    internal var currentAccounts by mutableStateOf<List<MutedAccount>>(emptyList())

    override fun onHidden() {
        // cleanup if needed
    }

    fun showMutedAccounts() {
        mutator.accept(MutedAccountAction.Load)
        show()
    }

    fun addAccount(did: ProfileId, handle: ProfileHandle) {
        mutator.accept(MutedAccountAction.Add(did, handle))
    }

    fun removeAccount(did: ProfileId) {
        mutator.accept(MutedAccountAction.Remove(did))
    }

    companion object {
        @Composable
        fun rememberMutedAccountsSheetState(
            mutator: MutedAccountStateHolder,
        ): MutedAccountsSheetState {
            val state = rememberBottomSheetState {
                MutedAccountsSheetState(
                    mutator = mutator,
                    scope = it,
                )
            }

            MutedAccountsBottomSheet(state = state)

            return state
        }
    }
}

@Composable
private fun MutedAccountsBottomSheet(state: MutedAccountsSheetState) {
    state.ModalBottomSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Muted accounts sheet placeholder")
        }
    }
}
