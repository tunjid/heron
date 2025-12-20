package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class State(
    val isLoading: Boolean = false,
    val mutedWords: List<MutedWordPreference> = emptyList(),
    val error: String? = null,
    val newWord: String = "",
    val newWordDuration: Instant? = null,
)

sealed class Action(
    val key: String,
) {
    data class Add(val word: String, val duration: Instant) : Action("Add")
    data class Remove(val value: String) : Action("Remove")
    object ClearAll : Action("ClearAll")
    data class UpdateNewWord(val word: String) : Action("UpdateNewWord")
    data class UpdateDuration(val duration: Instant?) : Action("UpdateDuration")
    object Load : Action("Load")
}

@Stable
class MutedWordsStateHolder(
    private val userDataRepository: UserDataRepository,
) {
    private val actions = MutableSharedFlow<Action>(extraBufferCapacity = 64)

    fun createMutator(scope: CoroutineScope): StateFlow<State> {
        val mutator = scope.actionStateFlowMutator<Action, State>(
            initialState = State(),
            started = SharingStarted.WhileSubscribed(),
            inputs = listOf(
                loadMutedWords(
                    userDataRepository = userDataRepository,
                ),
            ),
            actionTransform = transform@{ actions ->
                actions.toMutationStream(keySelector = Action::key) {
                    when (val type = type()) {
                        is Action.Add ->
                            type.flow.addMutations(
                                userDataRepository = userDataRepository,
                            )

                        is Action.Remove ->
                            type.flow.removeMutations(
                                userDataRepository = userDataRepository,
                            )

                        is Action.ClearAll ->
                            type.flow.clearAllMutations(
                                userDataRepository = userDataRepository,
                            )

                        is Action.Load ->
                            type.flow.loadMutations(
                                userDataRepository = userDataRepository,
                            )

                        is Action.UpdateNewWord ->
                            type.flow.updateNewWordMutations()

                        is Action.UpdateDuration ->
                            type.flow.updateDurationMutations()
                    }
                }
            },
        )

        scope.launch {
            actions.collect { action ->
                mutator.accept(action)
            }
        }

        return mutator.state
    }

    fun handleAction(action: Action, scope: CoroutineScope) {
        scope.launch {
            actions.emit(action)
        }
    }
}

private fun Flow<Action.Add>.addMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
        if (action.word.isBlank()) {
            emit(mutationOf { copy(error = "Word cannot be empty") })
            return@mapLatestToManyMutations
        }

        emit(mutationOf { copy(isLoading = true) })

        val preference = MutedWordPreference(
            value = action.word,
            targets = listOf(MutedWordPreference.Target("content")),
            actorTarget = null,
            expiresAt = action.duration,
        )

        try {
            userDataRepository.createMutedWord(preference)
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        newWord = "",
                        newWordDuration = null,
                        error = null,
                    )
                },
            )
        } catch (e: Exception) {
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = "Failed to add: ${e.message}",
                    )
                },
            )
        }
    }

private fun Flow<Action.Remove>.removeMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
        emit(mutationOf { copy(isLoading = true) })

        try {
            userDataRepository.removeMutedWord(action.value)
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = null,
                    )
                },
            )
        } catch (e: Exception) {
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = "Failed to remove: ${e.message}",
                    )
                },
            )
        }
    }

private fun Flow<Action.ClearAll>.clearAllMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations {
        emit(mutationOf { copy(isLoading = true) })

        try {
            userDataRepository.clearAllMutedWords()
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = null,
                    )
                },
            )
        } catch (e: Exception) {
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = "Failed to clear: ${e.message}",
                    )
                },
            )
        }
    }

private fun Flow<Action.Load>.loadMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations {
        emit(mutationOf { copy(isLoading = true) })

        try {
            userDataRepository.refreshPreferences()
            emit(
                mutationOf {
                    copy(isLoading = false, error = null)
                },
            )
        } catch (e: Exception) {
            emit(
                mutationOf {
                    copy(
                        isLoading = false,
                        error = "Failed to refresh: ${e.message}",
                    )
                },
            )
        }
    }

private fun Flow<Action.UpdateNewWord>.updateNewWordMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWord = action.word)
    }

private fun Flow<Action.UpdateDuration>.updateDurationMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWordDuration = action.duration)
    }

private fun loadMutedWords(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.mutedWords()
        .mapToMutation { mutedWordPreferences ->
            copy(
                isLoading = false,
                mutedWords = mutedWordPreferences,
                error = null,
            )
        }

@Stable
class MutedWordsSheetState private constructor(
    private val stateHolder: MutedWordsStateHolder,
    private val coroutineScope: CoroutineScope,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    private val uiStateFlow = stateHolder.createMutator(coroutineScope)
    val uiState: StateFlow<State> = uiStateFlow

    fun handleAction(action: Action) {
        stateHolder.handleAction(action, coroutineScope)
    }

    fun showMutedWordsSheet() {
        show()
    }

    fun hideMutedWordsSheet() {
        hide()
    }

    fun clearAll() {
        handleAction(Action.ClearAll)
    }

    override fun onHidden() {
        handleAction(Action.UpdateNewWord(""))
        handleAction(Action.UpdateDuration(null))
    }

    companion object {
        @Composable
        fun rememberMutedWordsSheetState(
            stateHolder: MutedWordsStateHolder,
        ): MutedWordsSheetState {
            val coroutineScope = rememberCoroutineScope()
            val state = rememberBottomSheetState {
                MutedWordsSheetState(
                    stateHolder = stateHolder,
                    coroutineScope = coroutineScope,
                    scope = it,
                )
            }
            MutedWordsBottomSheet(state)
            return state
        }
    }
}

@Composable
private fun MutedWordsBottomSheet(state: MutedWordsSheetState) {
    state.ModalBottomSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Muted words sheet placeholder")
        }
    }
}
