package com.tunjid.heron.sheets.postoptions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
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

@Stable
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
                        is PostOptionsAction.UpdatePostReference -> action.flow.launchUpdatePostReferenceMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is PostOptionsAction.SendFeedInteraction -> action.flow.launchSendFeedInteractionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is PostOptionsAction.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                            state = state,
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
    state: PostOptionsState.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.StandardSite.UpdatePostReference(it.reference) },
) { _, memo ->
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<PostOptionsAction.SendFeedInteraction>.launchSendFeedInteractionMutations(
    state: PostOptionsState.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.FeedInteraction(requests = listOf(it.request)) },
) { _, memo ->
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<PostOptionsAction.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: PostOptionsState.SnapshotMutable,
) = launchedCollect {
    state.messages -= it.message
}

@Stable
@Snapshottable
interface PostOptionsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        val recentConversations: List<Conversation> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : PostOptionsState
}

sealed class PostOptionsAction(val key: String) {
    data class UpdatePostReference(
        val reference: StandardDocument.PostReference,
    ) : PostOptionsAction(key = "UpdatePostReference")

    data class SendFeedInteraction(
        val request: FeedGenerator.Interaction.Request,
    ) : PostOptionsAction(key = "SendFeedInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : PostOptionsAction("SnackbarDismissed")
}
