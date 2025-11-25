package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MutedWord(
    val id: String,
    val word: String,
)

sealed class MutedWordAction {
    data object Load : MutedWordAction()
    data class Add(val word: String) : MutedWordAction()
    data class Remove(val id: String) : MutedWordAction()
}

typealias MutedWordStateHolder =
    ActionStateMutator<MutedWordAction, StateFlow<MutedWordState>>

data class MutedWordState(
    val isLoading: Boolean = false,
    val mutedWords: List<MutedWord> = emptyList(),
)

class MutedWordMutator : MutedWordStateHolder {

    private val _state = MutableStateFlow(MutedWordState())
    override val state: StateFlow<MutedWordState> = _state

    override val accept: (MutedWordAction) -> Unit = { action ->
        when (action) {
            MutedWordAction.Load -> loadWords()
            is MutedWordAction.Add -> addWord(action.word)
            is MutedWordAction.Remove -> removeWord(action.id)
        }
    }

    private fun loadWords() {
        _state.update { it.copy(isLoading = true) }
        // TODO: real API call
        _state.update {
            it.copy(
                isLoading = false,
                mutedWords = emptyList(),
            )
        }
    }

    private fun addWord(word: String) {
        // TODO: API call
        val newItem = MutedWord(id = word.hashCode().toString(), word = word)
        _state.update { it.copy(mutedWords = it.mutedWords + newItem) }
    }

    private fun removeWord(id: String) {
        _state.update {
            it.copy(mutedWords = it.mutedWords.filterNot { w -> w.id == id })
        }
    }
}

@Stable
class MutedWordsSheetState private constructor(
    val mutator: MutedWordMutator,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    internal var currentWords by mutableStateOf<List<MutedWord>>(emptyList())

    override fun onHidden() {
        // cleanup if needed
    }

    fun showMutedWords() {
        mutator.accept(MutedWordAction.Load)
        show()
    }

    fun addWord(word: String) {
        mutator.accept(MutedWordAction.Add(word))
    }

    fun removeWord(id: String) {
        mutator.accept(MutedWordAction.Remove(id))
    }

    companion object {
        @Composable
        fun rememberMutedWordsSheetState(
            mutator: MutedWordMutator,
        ): MutedWordsSheetState {
            val state = rememberBottomSheetState {
                MutedWordsSheetState(
                    mutator = mutator,
                    scope = it,
                )
            }

            MutedWordsBottomSheet(state = state)

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

/** Usage  in UI **/
// val mutator = remember { MutedWordMutator() }
// val mutedSheet = rememberMutedWordsSheetState(mutator)
//
