package com.tunjid.heron.timeline.ui.sheets.mutedwords

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.timeline.utilities.enqueueMutations
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.collections.plus
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.take
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias MutedWordsStateHolder = ActionStateMutator<MutedWordsAction, StateFlow<MutedWordsState>>

@AssistedFactory
fun interface MutedWordsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): MutedWordsViewModel
}

@AssistedInject
class MutedWordsViewModel(
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    MutedWordsStateHolder by scope.actionStateFlowMutator(
        initialState = MutedWordsState(),
        started = SharingStarted.WhileSubscribed(5_000),
        inputs = listOf(
            loadPreferencesMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = { actions ->
            actions.toMutationStream(keySelector = MutedWordsAction::key) {
                when (val action = type()) {
                    is MutedWordsAction.UpdateNewWord -> action.flow.updateNewWordMutations()
                    is MutedWordsAction.UpdateDuration -> action.flow.updateDurationMutations()
                    is MutedWordsAction.UpdateTargets -> action.flow.updateTargetsMutations()
                    is MutedWordsAction.UpdateExcludeNonFollowers -> action.flow.updateExcludeNonFollowersMutations()
                    is MutedWordsAction.AddMutedWord -> action.flow.addWordMutations()
                    is MutedWordsAction.RemoveMutedWord -> action.flow.removeMutedWordMutations()
                    is MutedWordsAction.ClearAll -> action.flow.clearAllMutations()
                    is MutedWordsAction.ResetErrors -> action.flow.resetErrorsMutations()
                    is MutedWordsAction.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

private fun loadPreferencesMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<MutedWordsState>> =
    userDataRepository.preferences
        .take(1) // only seed on first load, never overwrite local edits
        .mapToMutation {
            copy(mutedWords = it.mutedWordPreferences)
        }

private fun Flow<MutedWordsAction.UpdateNewWord>.updateNewWordMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(newWord = it.value) }

private fun Flow<MutedWordsAction.UpdateDuration>.updateDurationMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(newWordDuration = it.duration) }

private fun Flow<MutedWordsAction.UpdateTargets>.updateTargetsMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(newWordTargets = it.targets) }

private fun Flow<MutedWordsAction.UpdateExcludeNonFollowers>.updateExcludeNonFollowersMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(newWordExcludeNonFollowers = it.exclude) }

private fun Flow<MutedWordsAction.RemoveMutedWord>.removeMutedWordMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation {
        copy(
            mutedWords = mutedWords.filterNot { w -> w.value == it.value },
            error = null,
        )
    }

private fun Flow<MutedWordsAction.ClearAll>.clearAllMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(mutedWords = emptyList(), error = null) }

private fun Flow<MutedWordsAction.ResetErrors>.resetErrorsMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation { copy(error = null) }

private fun Flow<MutedWordsAction.AddMutedWord>.addWordMutations(): Flow<Mutation<MutedWordsState>> =
    mapToMutation {
        if (newWord.isBlank()) return@mapToMutation this

        if (mutedWords.any { it.value.contentEquals(newWord, ignoreCase = true) }) {
            return@mapToMutation copy(error = "Word already muted")
        }

        val expiresAt = newWordDuration?.let { Clock.System.now().plus(it) }
        copy(
            mutedWords = mutedWords + MutedWordPreference(
                value = newWord,
                targets = newWordTargets.map { MutedWordPreference.Target(it) },
                actorTarget = if (newWordExcludeNonFollowers)
                    MutedWordPreference.Target("non_followers") else null,
                expiresAt = expiresAt,
            ),
            newWord = "",
            newWordDuration = null,
            newWordTargets = listOf("content", "tag"),
            newWordExcludeNonFollowers = false,
            error = null,
        )
    }

private fun Flow<MutedWordsAction.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<MutedWordsState>> =
    this.enqueueMutations(
        writeQueue,
        toWritable = { action ->
            Writable.TimelineUpdate(
                Timeline.Update.OfMutedWord.ReplaceAll(
                    mutedWordPreferences = action.mutedWordPreference,
                ),
            )
        },
    ) { _, memo ->
        if (memo != null) emit { copy(messages = messages + memo) }
    }

@Serializable
data class MutedWordsState(
    val mutedWords: List<MutedWordPreference> = emptyList(),
    val newWord: String = "",
    val newWordTargets: List<String> = listOf("content", "tag"),
    val newWordDuration: Duration? = null,
    val newWordExcludeNonFollowers: Boolean = false,
    val preferencesLoaded: Boolean = false,
    val error: String? = null,
    @Transient
    val messages: List<Memo> = emptyList(),
)

sealed class MutedWordsAction(val key: String) {
    data class UpdateNewWord(
        val value: String,
    ) : MutedWordsAction("UpdateNewWord")
    data class UpdateDuration(
        val duration: Duration?,
    ) : MutedWordsAction("UpdateDuration")
    data class UpdateTargets(
        val targets: List<String>,
    ) : MutedWordsAction("UpdateTargets")
    data class UpdateExcludeNonFollowers(
        val exclude: Boolean,
    ) : MutedWordsAction("UpdateExcludeNonFollowers")
    data object AddMutedWord : MutedWordsAction("AddMutedWord")
    data class RemoveMutedWord(val value: String) : MutedWordsAction("RemoveMutedWord")
    data object ClearAll : MutedWordsAction("ClearAll")
    data object ResetErrors : MutedWordsAction("ResetErrors")

    data class UpdateMutedWord(
        val mutedWordPreference: List<MutedWordPreference>,
    ) : MutedWordsAction(key = "UpdateMutedWord")
}
