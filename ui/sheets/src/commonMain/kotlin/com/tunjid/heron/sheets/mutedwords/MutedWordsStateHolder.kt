package com.tunjid.heron.sheets.mutedwords

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.UserDataRepository
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
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.take
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface MutedWordsStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<MutedWordsAction, MutedWordsState>

@AssistedFactory
fun interface MutedWordsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): MutedWordsViewModel
}

class MutedWordsViewModel(
    mutator: ActionSuspendingStateMutator<MutedWordsAction, MutedWordsState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    MutedWordsStateHolder,
    ActionSuspendingStateMutator<MutedWordsAction, MutedWordsState> by mutator {

    @AssistedInject
    constructor(
        userDataRepository: UserDataRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = MutedWordsState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                launchLoadPreferencesMutations(
                    state = state,
                    userDataRepository = userDataRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = MutedWordsAction::key,
                ) {
                    when (val action = type()) {
                        is MutedWordsAction.UpdateNewWord -> action.flow.launchUpdateNewWordMutations(
                            state = state,
                        )
                        is MutedWordsAction.UpdateDuration -> action.flow.launchUpdateDurationMutations(
                            state = state,
                        )
                        is MutedWordsAction.UpdateTargets -> action.flow.launchUpdateTargetsMutations(
                            state = state,
                        )
                        is MutedWordsAction.UpdateExcludeNonFollowers -> action.flow.launchUpdateExcludeNonFollowersMutations(
                            state = state,
                        )
                        is MutedWordsAction.AddMutedWord -> action.flow.launchAddWordMutations(
                            state = state,
                        )
                        is MutedWordsAction.RemoveMutedWord -> action.flow.launchRemoveMutedWordMutations(
                            state = state,
                        )
                        is MutedWordsAction.ClearAll -> action.flow.launchClearAllMutations(
                            state = state,
                        )
                        is MutedWordsAction.ResetErrors -> action.flow.launchResetErrorsMutations(
                            state = state,
                        )
                        is MutedWordsAction.UpdateMutedWord -> action.flow.launchUpdateMutedWordMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is MutedWordsAction.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
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
private fun launchLoadPreferencesMutations(
    state: MutedWordsState.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences
    .take(1)
    .launchedCollect {
        state.mutedWords = it.mutedWordPreferences
        state.preferencesLoaded = true
    }

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.UpdateNewWord>.launchUpdateNewWordMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.newWord = it.value
    state.error = null
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.UpdateDuration>.launchUpdateDurationMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.newWordDuration = it.duration
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.UpdateTargets>.launchUpdateTargetsMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.newWordTargets = it.targets
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.UpdateExcludeNonFollowers>.launchUpdateExcludeNonFollowersMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.newWordExcludeNonFollowers = it.exclude
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.RemoveMutedWord>.launchRemoveMutedWordMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.mutedWords = state.mutedWords.filterNot { w -> w.value == it.value }
    state.error = null
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.ClearAll>.launchClearAllMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.mutedWords = emptyList()
    state.error = null
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.ResetErrors>.launchResetErrorsMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.error = null
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.AddMutedWord>.launchAddWordMutations(
    state: MutedWordsState.SnapshotMutable,
) =
    launchedCollect {
        val trimmedWord = state.newWord.trim()
        if (trimmedWord.isBlank()) return@launchedCollect

        if (state.mutedWords.any { it.value.contentEquals(trimmedWord, ignoreCase = true) }) {
            state.error = "Word already muted"
            return@launchedCollect
        }

        val expiresAt = state.newWordDuration?.let { Clock.System.now().plus(it) }
        state.update(
            mutedWords = state.mutedWords + MutedWordPreference(
                value = trimmedWord,
                targets = state.newWordTargets.map { MutedWordPreference.Target(it) },
                actorTarget = if (state.newWordExcludeNonFollowers)
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

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.UpdateMutedWord>.launchUpdateMutedWordMutations(
    state: MutedWordsState.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue,
    toWritable = { action ->
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = action.mutedWordPreference,
            ),
        )
    },
) { _, memo ->
    if (memo != null) state.messages += memo
}

context(productionScope: CoroutineScope)
private fun Flow<MutedWordsAction.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: MutedWordsState.SnapshotMutable,
) = launchedCollect {
    state.messages -= it.message
}

@Stable
@Snapshottable
interface MutedWordsState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        val mutedWords: List<MutedWordPreference> = emptyList(),
        val newWord: String = "",
        val newWordTargets: List<String> = listOf("content", "tag"),
        val newWordDuration: Duration? = null,
        val newWordExcludeNonFollowers: Boolean = false,
        val preferencesLoaded: Boolean = false,
        val error: String? = null,
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : MutedWordsState
}

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

    data class SnackbarDismissed(
        val message: Memo,
    ) : MutedWordsAction(key = "SnackbarDismissed")
}
