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

data class BlockedAccount(
    val did: ProfileId,
    val handle: ProfileHandle,
)

sealed class BlockedAccountAction {
    data object Load : BlockedAccountAction()
    data class Add(val did: ProfileId, val handle: ProfileHandle) : BlockedAccountAction()
    data class Remove(val did: ProfileId) : BlockedAccountAction()
}

data class BlockedAccountState(
    val isLoading: Boolean = false,
    val blockedAccounts: List<BlockedAccount> = emptyList(),
)

typealias BlockedAccountStateHolder =
    ActionStateMutator<BlockedAccountAction, StateFlow<BlockedAccountState>>

fun CoroutineScope.blockedAccountMutator(
    scope: CoroutineScope,
): BlockedAccountStateHolder =
    actionStateFlowMutator(
        initialState = BlockedAccountState(),
        actionTransform = { actions ->
            actions.toMutationStream {
                when (val action = type()) {
                    BlockedAccountAction.Load -> {
                        TODO("Implement Load action")
                    }

                    is BlockedAccountAction.Add -> {
                        TODO("Implement Add action")
                    }

                    is BlockedAccountAction.Remove -> {
                        TODO("Implement Remove action")
                    }
                }
            }
        },
    )

@Stable
class BlockedAccountsSheetState private constructor(
    val mutator: BlockedAccountStateHolder,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    internal var currentAccounts by mutableStateOf<List<BlockedAccount>>(emptyList())

    override fun onHidden() {
        // cleanup if needed
    }

    fun showBlockedAccounts() {
        mutator.accept(BlockedAccountAction.Load)
        show()
    }

    fun blockAccount(did: ProfileId, handle: ProfileHandle) {
        mutator.accept(BlockedAccountAction.Add(did, handle))
    }

    fun unblockAccount(did: ProfileId) {
        mutator.accept(BlockedAccountAction.Remove(did))
    }

    companion object {
        @Composable
        fun rememberBlockedAccountsSheetState(
            mutator: BlockedAccountStateHolder,
        ): BlockedAccountsSheetState {
            val state = rememberBottomSheetState {
                BlockedAccountsSheetState(
                    mutator = mutator,
                    scope = it,
                )
            }

            BlockedAccountsBottomSheet(state = state)

            return state
        }
    }
}

@Composable
private fun BlockedAccountsBottomSheet(state: BlockedAccountsSheetState) {
    state.ModalBottomSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Blocked accounts sheet placeholder")
        }
    }
}
