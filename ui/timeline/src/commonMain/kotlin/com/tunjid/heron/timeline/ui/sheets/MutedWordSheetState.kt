package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.timeline.ui.moderation.ModerationFeatureWhileSubscribed
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.actor_target
import heron.ui.timeline.generated.resources.add_muted_word
import heron.ui.timeline.generated.resources.add_words_above_to_mute
import heron.ui.timeline.generated.resources.clear_all
import heron.ui.timeline.generated.resources.clear_icon
import heron.ui.timeline.generated.resources.close_icon
import heron.ui.timeline.generated.resources.duration
import heron.ui.timeline.generated.resources.error
import heron.ui.timeline.generated.resources.exclude_non_followers_txt
import heron.ui.timeline.generated.resources.exclude_user_txt
import heron.ui.timeline.generated.resources.forever
import heron.ui.timeline.generated.resources.mute_in
import heron.ui.timeline.generated.resources.mute_user_txt
import heron.ui.timeline.generated.resources.mute_words
import heron.ui.timeline.generated.resources.mute_words_placeholder
import heron.ui.timeline.generated.resources.mute_words_title
import heron.ui.timeline.generated.resources.no_muted_words_yet
import heron.ui.timeline.generated.resources.remove
import heron.ui.timeline.generated.resources.selected
import heron.ui.timeline.generated.resources.seven_days
import heron.ui.timeline.generated.resources.tags_only
import heron.ui.timeline.generated.resources.targets
import heron.ui.timeline.generated.resources.text_tags
import heron.ui.timeline.generated.resources.thirty_days
import heron.ui.timeline.generated.resources.twenty_four_hours
import heron.ui.timeline.generated.resources.unselected
import heron.ui.timeline.generated.resources.your_muted_word
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data class State(
    val isLoading: Boolean = false,
    val error: String? = null,
    val newWord: String = "",
    val newWordTargets: List<String> = listOf("content", "tag"),
    val newWordDuration: Instant? = null,
    val newWordExcludeNonFollowers: Boolean = false,
    @Transient
    val mutedWords: List<MutedWordPreference> = emptyList(),
)

sealed class Action(
    val key: String,
) {
    data class Add(
        val value: String,
        val duration: Instant,
        val targets: List<String>,
        val excludeNonFollowers: Boolean,
    ) : Action("Add")

    data class Remove(
        val value: String,
    ) : Action("Remove")

    object ClearAll : Action("ClearAll")

    data class UpdateNewWord(
        val word: String,
    ) : Action("UpdateNewWord")

    data class UpdateDuration(
        val duration: Instant?,
    ) : Action("UpdateDuration")

    data class UpdateTargets(
        val targets: List<String>,
    ) : Action("UpdateTargets")

    data class UpdateExcludeNonFollowers(
        val exclude: Boolean,
    ) : Action("UpdateExcludeNonFollowers")
}

@Stable
class MutedWordsSheetState(
    private val coroutineScope: CoroutineScope,
    scope: BottomSheetScope,
    preferences: Flow<Preferences>,
    private val onSave: suspend (List<MutedWordPreference>) -> Unit,
) : BottomSheetState(scope),
    ActionStateMutator<Action, StateFlow<State>> by coroutineScope.actionStateFlowMutator(
        initialState = State(),
        inputs = listOf(
            preferencesInput(preferences),
        ),
        started = SharingStarted.WhileSubscribed(ModerationFeatureWhileSubscribed),
        actionTransform = { actions ->
            actions.toMutationStream(keySelector = Action::key) {
                when (val type = type()) {
                    is Action.Add -> type.flow.addMutations()
                    is Action.Remove -> type.flow.removeMutations()
                    is Action.ClearAll -> type.flow.clearAllMutations()
                    is Action.UpdateNewWord ->
                        type.flow.updateNewWordMutations()
                    is Action.UpdateDuration ->
                        type.flow.updateDurationMutations()
                    is Action.UpdateTargets ->
                        type.flow.updateTargetsMutations()
                    is Action.UpdateExcludeNonFollowers ->
                        type.flow.updateExcludeNonFollowersMutations()
                }
            }
        },
    ) {

    fun save() {
        coroutineScope.launch {
            onSave(state.value.mutedWords)
        }
    }

    override fun onHidden() {
        // Reset all fields to empty
        accept(Action.UpdateNewWord(""))
        accept(Action.UpdateDuration(null))
        accept(Action.UpdateExcludeNonFollowers(false))
        accept(Action.UpdateTargets(emptyList()))
    }

    companion object {
        @Composable fun rememberMutedWordsSheetState(
            preferences: Flow<Preferences>,
            onSave: suspend (List<MutedWordPreference>) -> Unit,
        ): MutedWordsSheetState {
            val coroutineScope = rememberCoroutineScope()
            val state = rememberBottomSheetState { scope ->
                MutedWordsSheetState(
                    coroutineScope = coroutineScope,
                    preferences = preferences,
                    onSave = onSave,
                    scope = scope,
                )
            }
            MutedWordsBottomSheet(state)
            return state
        }
    }
}

private fun preferencesInput(
    preferences: Flow<Preferences>,
): Flow<Mutation<State>> =
    preferences.mapToMutation { prefs ->
        copy(
            mutedWords = prefs.mutedWordPreferences,
        )
    }

private fun Flow<Action.Add>.addMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        val pref = MutedWordPreference(
            value = action.value,
            targets = action.targets.map { MutedWordPreference.Target(it) },
            actorTarget =
            if (action.excludeNonFollowers)
                MutedWordPreference.Target("non_followers")
            else null,
            expiresAt = action.duration,
        )

        if (mutedWords.any { it.value == action.value }) {
            copy(error = "Word already muted")
        } else {
            copy(
                mutedWords = mutedWords + pref,
                newWord = "",
                newWordDuration = null,
                error = null,
            )
        }
    }

private fun Flow<Action.Remove>.removeMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(
            mutedWords = mutedWords.filterNot { it.value == action.value },
            error = null,
        )
    }

private fun Flow<Action.ClearAll>.clearAllMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(mutedWords = emptyList(), error = null)
    }

private fun Flow<Action.UpdateNewWord>.updateNewWordMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWord = action.word)
    }

private fun Flow<Action.UpdateDuration>.updateDurationMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWordDuration = action.duration)
    }

private fun Flow<Action.UpdateTargets>.updateTargetsMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWordTargets = action.targets)
    }

private fun Flow<Action.UpdateExcludeNonFollowers>.updateExcludeNonFollowersMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(newWordExcludeNonFollowers = action.exclude)
    }

@Composable
private fun MutedWordsBottomSheet(
    sheetState: MutedWordsSheetState,
    modifier: Modifier = Modifier,
) {
    val uiState by sheetState.state.collectAsStateWithLifecycle()

    sheetState.ModalBottomSheet {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.mute_words),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                IconButton(
                    onClick = { sheetState.hide() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close_icon),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            uiState.error?.let { error ->
                ErrorMessage(error = error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        stringResource(Res.string.mute_words_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    OutlinedTextField(
                        value = uiState.newWord,
                        onValueChange = {
                            sheetState.accept(Action.UpdateNewWord(it))
                        },
                        placeholder = { Text(stringResource(Res.string.mute_words_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        isError = uiState.error != null,
                        trailingIcon = {
                            if (uiState.newWord.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        sheetState.accept(Action.UpdateNewWord(""))
                                    },
                                    enabled = !uiState.isLoading,
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(Res.string.clear_icon),
                                    )
                                }
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(Res.string.duration),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val durationOptions = remember {
                        listOf(
                            DurationOption(Res.string.forever, null),
                            DurationOption(Res.string.twenty_four_hours, Clock.System.now().plus(Duration.parse("24h"))),
                            DurationOption(Res.string.seven_days, Clock.System.now().plus(Duration.parse("7d"))),
                            DurationOption(Res.string.thirty_days, Clock.System.now().plus(Duration.parse("30d"))),
                        )
                    }

                    val selectedDuration = uiState.newWordDuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        durationOptions.forEach { option ->
                            DurationChip(
                                option = option,
                                isSelected = selectedDuration == option.expiresAt,
                                onSelected = {
                                    sheetState.accept(
                                        Action.UpdateDuration(option.expiresAt),
                                    )
                                },
                                isLoading = uiState.isLoading,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(Res.string.mute_in),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val targets = remember {
                        listOf(
                            MuteTargetOption(Res.string.text_tags, listOf("content", "tag")),
                            MuteTargetOption(Res.string.tags_only, listOf("tag")),
                        )
                    }

                    val selectedTargets = uiState.newWordTargets

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        targets.forEach { target ->
                            MuteTargetChip(
                                target = target,
                                isSelected = selectedTargets.containsAll(target.targets) &&
                                    target.targets.containsAll(selectedTargets),
                                onSelected = {
                                    sheetState.accept(
                                        Action.UpdateTargets(target.targets),
                                    )
                                },
                                isLoading = uiState.isLoading,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(Res.string.exclude_user_txt),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(Res.string.mute_user_txt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Switch(
                            checked = uiState.newWordExcludeNonFollowers,
                            onCheckedChange = { checked ->
                                sheetState.accept(
                                    Action.UpdateExcludeNonFollowers(checked),
                                )
                            },
                            enabled = !uiState.isLoading,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val duration = selectedDuration ?: Instant.DISTANT_FUTURE
                            sheetState.accept(
                                Action.Add(
                                    value = uiState.newWord,
                                    duration = duration,
                                    targets = uiState.newWordTargets,
                                    excludeNonFollowers = uiState.newWordExcludeNonFollowers,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.newWord.isNotBlank() &&
                            selectedTargets.isNotEmpty() &&
                            !uiState.isLoading,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                stringResource(Res.string.add_muted_word),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.your_muted_word),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (uiState.mutedWords.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    sheetState.accept(Action.ClearAll)
                                },
                                enabled = !uiState.isLoading,
                            ) {
                                Text(
                                    stringResource(Res.string.clear_all),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isLoading && uiState.mutedWords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.mutedWords.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uiState.mutedWords, key = { it.value }) { mutedWord ->
                                MutedWordItem(
                                    mutedWord = mutedWord,
                                    onRemove = {
                                        sheetState.accept(Action.Remove(mutedWord.value))
                                    },
                                    isLoading = uiState.isLoading,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    option: DurationOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    isLoading: Boolean,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = { if (!isLoading) onSelected() },
        enabled = !isLoading,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = stringResource(Res.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(Res.string.unselected),
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(option.label),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MuteTargetChip(
    target: MuteTargetOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    isLoading: Boolean,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = { if (!isLoading) onSelected() },
        enabled = !isLoading,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(Res.string.selected),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = stringResource(Res.string.unselected),
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(target.label),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MutedWordItem(
    mutedWord: MutedWordPreference,
    onRemove: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mutedWord.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                IconButton(
                    onClick = onRemove,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.remove),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = stringResource(Res.string.duration),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formatDuration(mutedWord.expiresAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = stringResource(Res.string.targets),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formatTargets(mutedWord.targets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            mutedWord.actorTarget?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(Res.string.actor_target),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = stringResource(Res.string.exclude_non_followers_txt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.no_muted_words_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.add_words_above_to_mute),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(Res.string.error),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun formatDuration(expiresAt: Instant?): String {
    return expiresAt?.let {
        if (it == Instant.DISTANT_FUTURE) {
            "Forever"
        } else {
            val now = Clock.System.now()
            if (it > now) {
                val duration = it - now
                val totalHours = duration.inWholeHours
                val days = totalHours / 24
                val hours = totalHours % 24

                when {
                    days > 0 -> "Expires in ${days}d ${hours}h"
                    hours > 0 -> "Expires in ${hours}h"
                    else -> "Expires soon"
                }
            } else {
                "Expired"
            }
        }
    } ?: "Forever"
}

private fun formatTargets(targets: List<MutedWordPreference.Target>): String {
    return when {
        targets.size == 2 -> "Text & tags"
        targets.size == 1 && targets.first().value == "content" -> "Text only"
        targets.size == 1 && targets.first().value == "tag" -> "Tags only"
        else -> targets.joinToString(", ") { it.value }
    }
}

private data class DurationOption(
    val label: StringResource,
    val expiresAt: Instant?,
)

private data class MuteTargetOption(
    val label: StringResource,
    val targets: List<String>,
)
