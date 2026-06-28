package com.tunjid.heron.sheets.postoptions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
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

interface PostOptionsStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<PostOptionsAction, PostOptionsState>

@AssistedFactory
fun interface PostOptionsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): PostOptionsViewModel
}

class PostOptionsViewModel(
    mutator: ActionSuspendingStateMutator<PostOptionsAction, PostOptionsState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    PostOptionsStateHolder,
    ActionSuspendingStateMutator<PostOptionsAction, PostOptionsState> by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        messageRepository: MessageRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = PostOptionsState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                launchLoadSignedInProfileMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchUpdateRecentConversionsMutations(
                    state = state,
                    messageRepository = messageRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = PostOptionsAction::key,
                ) {
                    when (val action = type()) {
                        is PostOptionsAction.UpdatePostReference ->
                            action.flow.launchUpdatePostReferenceMutations(
                                writeQueue = writeQueue,
                            )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileMutations(
    state: PostOptionsState.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .distinctUntilChanged()
    .launchedCollect {
        state.signedInProfileId = it?.did
    }

context(productionScope: CoroutineScope)
private fun launchUpdateRecentConversionsMutations(
    state: PostOptionsState.SnapshotMutable,
    messageRepository: MessageRepository,
) = messageRepository.recentConversations()
    .launchedCollect {
        state.recentConversations = it
    }

context(productionScope: CoroutineScope)
private fun Flow<PostOptionsAction.UpdatePostReference>.launchUpdatePostReferenceMutations(
    writeQueue: WriteQueue,
) = launchedCollect { action ->
    writeQueue.enqueue(Writable.StandardSite.UpdatePostReference(action.reference))
}

@Stable
@Snapshottable
interface PostOptionsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        val recentConversations: List<Conversation> = emptyList(),
    ) : PostOptionsState
}

sealed class PostOptionsAction(val key: String) {
    data class UpdatePostReference(
        val reference: StandardDocument.PostReference,
    ) : PostOptionsAction(key = "UpdatePostReference")
}
