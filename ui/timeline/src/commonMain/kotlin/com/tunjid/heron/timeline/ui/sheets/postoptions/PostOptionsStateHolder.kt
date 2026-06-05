package com.tunjid.heron.timeline.ui.sheets.postoptions

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.timeline.utilities.SheetWhileSubscribed
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable

typealias PostOptionsStateHolder = ActionStateMutator<PostOptionsAction, StateFlow<PostOptionsState>>

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
    PostOptionsStateHolder by scope.actionStateFlowMutator(
        initialState = PostOptionsState(),
        started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
        inputs = listOf(
            loadSignedInProfileMutations(
                authRepository = authRepository,
            ),
        ),
        actionTransform = { actions ->
            actions.toMutationStream(keySelector = PostOptionsAction::key) {
                when (val action = type()) {
                    is PostOptionsAction.UpdateRecentConversations -> action.flow.updateRecentConversionsMutations(
                        messageRepository = messageRepository,
                    )
                }
            }
        },
    )

private fun loadSignedInProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<PostOptionsState>> =
    authRepository.signedInUser
        .distinctUntilChanged()
        .mapToMutation { copy(signedInProfileId = it?.did) }

private fun Flow<PostOptionsAction.UpdateRecentConversations>.updateRecentConversionsMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<PostOptionsState>> =
    messageRepository.recentConversations()
        .mapToMutation { copy(recentConversations = it) }

@Serializable
data class PostOptionsState(
    val signedInProfileId: ProfileId? = null,
    val recentConversations: List<Conversation> = emptyList(),
)

sealed class PostOptionsAction(val key: String) {
    data object UpdateRecentConversations : PostOptionsAction(key = "PostOptionsAction")
}
