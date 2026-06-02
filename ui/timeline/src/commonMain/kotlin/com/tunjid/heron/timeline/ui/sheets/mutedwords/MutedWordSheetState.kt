package com.tunjid.heron.timeline.ui.sheets.mutedwords

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.ui.coroutines.UIStateProducerElement
import com.tunjid.heron.ui.coroutines.viewModelCoroutineScope
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.add_muted_word
import heron.ui.timeline.generated.resources.add_words_above_to_mute
import heron.ui.timeline.generated.resources.clear_all
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
import heron.ui.timeline.generated.resources.seven_days
import heron.ui.timeline.generated.resources.tags_only
import heron.ui.timeline.generated.resources.text_tags
import heron.ui.timeline.generated.resources.thirty_days
import heron.ui.timeline.generated.resources.twenty_four_hours
import heron.ui.timeline.generated.resources.your_muted_word
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class MutedWordsSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    override fun onHidden() { }

    companion object {
        @Composable
        fun rememberUpdatedMutedWordsSheetState(
            initializer: MutedWordsViewModelInitializer,
        ): MutedWordsSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = false,
            ) { scope -> MutedWordsSheetState(scope = scope) }

            val storeProvider = rememberViewModelStoreProvider()

            MutedWordsBottomSheet(
                state = state,
                storeProvider = storeProvider,
                initializer = initializer,
            )
            return state
        }
    }
}

@Composable
private fun MutedWordsBottomSheet(
    state: MutedWordsSheetState,
    storeProvider: ViewModelStoreProvider,
    initializer: MutedWordsViewModelInitializer,
) {
    val storeOwner = rememberViewModelStoreOwner(
        provider = storeProvider,
        key = "muted_words_sheet",
    )

    state.ModalBottomSheet {
        CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
            MutedWordsScreen(
                sheetState = state,
                initializer = initializer,
            )
        }
    }
}

@Composable
private fun MutedWordsScreen(
    sheetState: MutedWordsSheetState,
    initializer: MutedWordsViewModelInitializer,
) {
    val stateHolder: MutedWordsStateHolder = viewModel<MutedWordsViewModel> {
        initializer.invoke(
            scope = viewModelCoroutineScope(),
        )
    }

    val state by stateHolder.state.collectAsStateWithLifecycle()

    val initialMutedWords = remember { androidx.compose.runtime.mutableStateOf<List<MutedWordPreference>?>(null) }
    if (state.preferencesLoaded && initialMutedWords.value == null) {
        initialMutedWords.value = state.mutedWords
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentWords = stateHolder.state.value.mutedWords
            if (initialMutedWords.value != null && initialMutedWords.value != currentWords) {
                stateHolder.accept(
                    MutedWordsAction.UpdateMutedWord(currentWords),
                )
            }
        }
    }

    MutedWordsContent(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        sheetState = sheetState,
        state = state,
        actions = stateHolder.accept,
    )
}

@Composable
private fun MutedWordsContent(
    modifier: Modifier = Modifier,
    sheetState: MutedWordsSheetState,
    state: MutedWordsState,
    actions: (MutedWordsAction) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = MainTitleKey) {
            Row(
                modifier = Modifier.fillMaxWidth().animateItem(),
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
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        }

        state.error?.let { error ->
            item(key = ErrorKey) {
                ErrorMessage(modifier = Modifier.animateItem(), error = error)
            }
        }

        item(
            key = InputKey,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(Res.string.mute_words_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    TextField(
                        value = state.newWord,
                        onValueChange = { actions(MutedWordsAction.UpdateNewWord(it)) },
                        placeholder = {
                            Text(
                                stringResource(Res.string.mute_words_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.error != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        trailingIcon = {
                            if (state.newWord.isNotBlank()) {
                                IconButton(onClick = { actions(MutedWordsAction.UpdateNewWord("")) }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions {
                            actions(MutedWordsAction.AddMutedWord)
                        },
                    )

                    Text(
                        stringResource(Res.string.duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        durationOptions.forEach { option ->
                            DurationChip(
                                option = option,
                                isSelected = state.newWordDuration == option.expiresAt,
                                onSelected = {
                                    actions(MutedWordsAction.UpdateDuration(option.expiresAt))
                                },
                            )
                        }
                    }

                    Text(
                        stringResource(Res.string.mute_in),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    actions(MutedWordsAction.UpdateTargets(target.targets))
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.exclude_user_txt),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(Res.string.mute_user_txt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Switch(
                            checked = state.newWordExcludeNonFollowers,
                            onCheckedChange = {
                                actions(MutedWordsAction.UpdateExcludeNonFollowers(it))
                            },
                        )
                    }

                    Button(
                        onClick = {
                            actions(MutedWordsAction.AddMutedWord)
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
        }

        item(key = MutedWordsSubtitleKey) {
            Row(
                modifier = Modifier.fillMaxWidth().animateItem(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.your_muted_word),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.mutedWords.isNotEmpty()) {
                    TextButton(
                        onClick = { actions(MutedWordsAction.ClearAll) },
                    ) {
                        Text(
                            stringResource(Res.string.clear_all),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        if (state.mutedWords.isEmpty()) {
            item(key = EmptyKey) { EmptyState(modifier = Modifier.animateItem()) }
        } else {
            items(items = state.mutedWords, key = { it.value }) { mutedWord ->
                MutedWordItem(
                    modifier = Modifier.animateItem(),
                    mutedWord = mutedWord,
                    onRemove = { actions(MutedWordsAction.RemoveMutedWord(mutedWord.value)) },
                )
            }
        }

        item(key = BottomPaddingKey) {
            Spacer(
                modifier = Modifier
                    .height(40.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            )
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
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = onSelected,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )

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
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (isSelected)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = onSelected,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor,
                )
            }

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
    modifier: Modifier = Modifier,
    mutedWord: MutedWordPreference,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = mutedWord.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = buildMutedWordMeta(mutedWord),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedIconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
private fun ErrorMessage(
    modifier: Modifier = Modifier,
    error: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun buildMutedWordMeta(
    mutedWord: MutedWordPreference,
): String {
    val target = formatTargets(mutedWord.targets)
    val duration = formatDuration(mutedWord.expiresAt)

    return buildString {
        append(target)
        append(" \u00B7 ")
        append(duration)

        if (mutedWord.actorTarget != null) {
            append(" \u00B7 ")
            append(stringResource(Res.string.exclude_non_followers_txt))
        }
    }
}

private fun formatDuration(expiresAt: Instant?): String {
    return expiresAt?.let {
        if (it == Instant.DISTANT_FUTURE) {
            "Permanent"
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
    } ?: "Permanent"
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

private const val MainTitleKey = "com.tunjid.heron.title_key"
private const val InputKey = "com.tunjid.heron.input"
private const val ErrorKey = "com.tunjid.heron.error"
private const val EmptyKey = "com.tunjid.heron.empty"
private const val MutedWordsSubtitleKey = "com.tunjid.heron.muted_words_subtitle"
private const val BottomPaddingKey = "com.tunjid.heron.bottom_padding"
