package com.tunjid.heron.timeline.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class MutedWordsSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    var mutedWords by mutableStateOf<List<MutedWordPreference>>(emptyList())
        internal set

    var newWord by mutableStateOf("")
    var newWordTargets by mutableStateOf(listOf("content", "tag"))
    var newWordDuration by mutableStateOf<Duration?>(null)
    var newWordExcludeNonFollowers by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun addMutedWord() {
        val duration = newWordDuration
        val expiresAt = duration?.let {
            Clock.System.now().plus(it)
        }

        if (mutedWords.any { it.value == newWord }) {
            error = "Word already muted"
            return
        }

        mutedWords = mutedWords + MutedWordPreference(
            value = newWord,
            targets = newWordTargets.map { MutedWordPreference.Target(it) },
            actorTarget =
            if (newWordExcludeNonFollowers)
                MutedWordPreference.Target("non_followers")
            else null,
            expiresAt = expiresAt,
        )

        // Reset input fields
        newWord = ""
        newWordDuration = null
        error = null
    }

    fun removeMutedWord(value: String) {
        mutedWords = mutedWords.filterNot { it.value == value }
        error = null
    }

    fun clearAll() {
        mutedWords = emptyList()
        error = null
    }

    override fun onHidden() {
        newWord = ""
        newWordDuration = null
        newWordExcludeNonFollowers = false
        newWordTargets = listOf("content", "tag")
        error = null
    }

    companion object {

        @Composable
        fun rememberMutedWordsSheetState(
            mutedWords: List<MutedWordPreference>,
            onSave: (List<MutedWordPreference>) -> Unit,
            onShown: () -> Unit,
        ): MutedWordsSheetState {
            val state = rememberBottomSheetState { scope ->
                MutedWordsSheetState(
                    scope = scope,
                )
            }.also {
                it.mutedWords = mutedWords
            }

            MutedWordsBottomSheet(
                state = state,
                onShown = onShown,
                onSave = { onSave(state.mutedWords) },
            )
            return state
        }
    }
}

@Composable
private fun MutedWordsBottomSheet(
    modifier: Modifier = Modifier,
    state: MutedWordsSheetState,
    onSave: (List<MutedWordPreference>) -> Unit,
    onShown: () -> Unit,
) {
    state.ModalBottomSheet {
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
                    onClick = { state.hide() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close_icon),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            state.error?.let { error ->
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
                        value = state.newWord,
                        onValueChange = {
                            state.newWord = it
                        },
                        placeholder = { Text(stringResource(Res.string.mute_words_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.error != null,
                        trailingIcon = {
                            if (state.newWord.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        state.newWord = ""
                                    },
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
                            DurationOption(Res.string.twenty_four_hours, Duration.parse("24h")),
                            DurationOption(Res.string.seven_days, Duration.parse("7d")),
                            DurationOption(Res.string.thirty_days, Duration.parse("30d")),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        durationOptions.forEach { option ->
                            DurationChip(
                                option = option,
                                isSelected = state.newWordDuration == option.expiresAt,
                                onSelected = {
                                    state.newWordDuration = option.expiresAt
                                },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        targets.forEach { target ->
                            MuteTargetChip(
                                target = target,
                                isSelected =
                                state.newWordTargets.containsAll(target.targets) &&
                                    target.targets.containsAll(state.newWordTargets),
                                onSelected = {
                                    state.newWordTargets = target.targets
                                },
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
                            checked = state.newWordExcludeNonFollowers,
                            onCheckedChange = {
                                state.newWordExcludeNonFollowers = it
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            state.addMutedWord()
                            onSave(state.mutedWords)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.newWord.isNotBlank() &&
                            state.newWordTargets.isNotEmpty(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            stringResource(Res.string.add_muted_word),
                            style = MaterialTheme.typography.labelLarge,
                        )
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

                        if (state.mutedWords.isNotEmpty()) {
                            TextButton(onClick = { state.clearAll() }) {
                                Text(
                                    stringResource(Res.string.clear_all),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.mutedWords.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.mutedWords, key = { it.value }) { mutedWord ->
                                MutedWordItem(
                                    mutedWord = mutedWord,
                                    onRemove = {
                                        state.removeMutedWord(mutedWord.value)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        DisposableEffect(Unit) {
            onShown()
            onDispose { }
        }
    }
}

@Composable
private fun DurationChip(
    option: DurationOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
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
        onClick = { onSelected() },
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
        onClick = { onSelected() },
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
                    days > 0 && hours > 0 -> "Expires in ${days}d ${hours}h"
                    days > 0 -> "Expires in ${days}d"
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
    return when (targets.map { it.value }.toSet()) {
        setOf("content", "tag") -> "Text & tags"
        setOf("content") -> "Text only"
        setOf("tag") -> "Tags only"
        else -> targets.joinToString(", ") { it.value }
    }
}

private data class DurationOption(
    val label: StringResource,
    val expiresAt: Duration?,
)

private data class MuteTargetOption(
    val label: StringResource,
    val targets: List<String>,
)
