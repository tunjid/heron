package com.tunjid.heron.timeline.ui.sheets.embedrecordoptions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.timeline.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable

typealias EmbeddableRecordOptionsStateHolder =
    ActionSuspendingStateMutator<EmbeddableRecordOptionsAction, EmbeddableRecordOptionsState>

@AssistedFactory
fun interface EmbeddableRecordOptionsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): EmbeddableRecordOptionsViewModel
}

@AssistedInject
class EmbeddableRecordOptionsViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    EmbeddableRecordOptionsStateHolder by scope.actionSuspendingStateMutator(
        state = EmbeddableRecordOptionsState.Immutable().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
        producer = { state, actions ->
            launchLoadSignedInProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            launchLoadRecentConversationsMutations(
                state = state,
                messageRepository = messageRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = EmbeddableRecordOptionsAction::key,
            ) {
                when (val action = type()) {
                    is EmbeddableRecordOptionsAction.SetEditTitle -> action.flow.launchAndCollect {
                        state.editTitle = it.title
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileMutations(
    state: EmbeddableRecordOptionsState.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .distinctUntilChanged()
    .launchAndCollect { state.signedInProfileId = it?.did }

context(productionScope: CoroutineScope)
private fun launchLoadRecentConversationsMutations(
    state: EmbeddableRecordOptionsState.SnapshotMutable,
    messageRepository: MessageRepository,
) = messageRepository.recentConversations()
    .launchAndCollect { state.recentConversations = it }

@Stable
@Snapshottable
interface EmbeddableRecordOptionsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        val recentConversations: List<Conversation> = emptyList(),
        val editTitle: String? = null,
    ) : EmbeddableRecordOptionsState
}

sealed class EmbeddableRecordOptionsAction(val key: String) {
    data class SetEditTitle(val title: String?) : EmbeddableRecordOptionsAction("SetEditTitle")
}
