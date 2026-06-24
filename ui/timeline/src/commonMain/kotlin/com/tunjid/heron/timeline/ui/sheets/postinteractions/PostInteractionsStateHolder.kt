package com.tunjid.heron.timeline.ui.sheets.postinteractions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    writeQueue: WriteQueue,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    PostInteractionsStateHolder by scope.actionSuspendingStateMutator(
        state = PostInteractionsState.Immutable().toSnapshotMutable(),
        // Produced eagerly so the immediate-confirm path (like/bookmark) can read isSignedIn and
        // dispatch SendInteraction while the sheet is still hidden — that only works if the producer
        // is always running. Because collection is eager, take care adding flows here: they are
        // collected for the ViewModel's whole lifetime. authRepository.signedInUser is a hot, shared
        // (multicast) flow, so collecting it eagerly is cheap.
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            launchLoadSignedInMutations(
                state = state,
                authRepository = authRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = PostInteractionsAction::key,
            ) {
                when (val action = type()) {
                    is PostInteractionsAction.SendInteraction -> action.flow.launchSendInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is PostInteractionsAction.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )
                }
            }
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

context(productionScope: CoroutineScope)
private fun Flow<PostInteractionsAction.SendInteraction>.launchSendInteractionMutations(
    state: PostInteractionsState.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
) { _, memo ->
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<PostInteractionsAction.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: PostInteractionsState.SnapshotMutable,
) = launchedCollect {
    state.messages -= it.message
}

@Stable
@Snapshottable
interface PostInteractionsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val isSignedIn: Boolean = false,
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : PostInteractionsState
}

sealed class PostInteractionsAction(val key: String) {
    data class SendInteraction(
        val interaction: Post.Interaction,
    ) : PostInteractionsAction("SendInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : PostInteractionsAction("SnackbarDismissed")
}
