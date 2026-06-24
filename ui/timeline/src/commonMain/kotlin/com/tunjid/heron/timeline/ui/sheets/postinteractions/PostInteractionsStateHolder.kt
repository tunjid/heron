package com.tunjid.heron.timeline.ui.sheets.postinteractions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsState
import com.tunjid.heron.timeline.utilities.SheetWhileSubscribed
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable

typealias PostInteractionsStateHolder =
    ActionSuspendingStateMutator<PostInteractionsAction, PostInteractionsState>

@AssistedFactory
fun interface PostInteractionsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): PostInteractionsViewModel
}

@AssistedInject
class PostInteractionsViewModel(
    authRepository: AuthRepository,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    PostInteractionsStateHolder by scope.actionSuspendingStateMutator(
        state = PostInteractionsState.Immutable().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
        producer = { state, _ ->
            launchLoadSignedInMutations(
                state = state,
                authRepository = authRepository,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadSignedInMutations(
    state: PostInteractionsState.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .distinctUntilChanged()
    .launchedCollect {
        state.isSignedIn = it != null
    }

@Stable
@Snapshottable
interface PostInteractionsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val isSignedIn: Boolean = false,
    ) : PostInteractionsState
}

sealed class PostInteractionsAction(val key: String)
