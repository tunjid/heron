package com.tunjid.heron.timeline.ui.sheets.postoptions

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
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable

typealias PostOptionsStateHolder = ActionSuspendingStateMutator<PostOptionsAction, PostOptionsState>

@AssistedFactory
fun interface PostOptionsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): PostOptionsViewModel
}

@AssistedInject
class PostOptionsViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    PostOptionsStateHolder by scope.actionSuspendingStateMutator(
        state = PostOptionsState.Immutable().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
        producer = { state, _ ->
            launchLoadSignedInProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            launchUpdateRecentConversionsMutations(
                state = state,
                messageRepository = messageRepository,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileMutations(
    state: PostOptionsState.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .distinctUntilChanged()
    .launchAndCollect {
        state.signedInProfileId = it?.did
    }

context(productionScope: CoroutineScope)
private fun launchUpdateRecentConversionsMutations(
    state: PostOptionsState.SnapshotMutable,
    messageRepository: MessageRepository,
) = messageRepository.recentConversations()
    .launchAndCollect {
        state.recentConversations = it
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

sealed class PostOptionsAction(val key: String)
