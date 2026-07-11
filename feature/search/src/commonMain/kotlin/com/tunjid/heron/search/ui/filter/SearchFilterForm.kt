/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.search.ui.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.sheets.rememberProfileSearchSheetState
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.filter_add_people
import heron.feature.search.generated.resources.filter_add_people_filter
import heron.feature.search.generated.resources.filter_all_words
import heron.feature.search.generated.resources.filter_cancel
import heron.feature.search.generated.resources.filter_clear_date
import heron.feature.search.generated.resources.filter_date_prompt
import heron.feature.search.generated.resources.filter_exact_phrase
import heron.feature.search.generated.resources.filter_exact_phrase_hint
import heron.feature.search.generated.resources.filter_from
import heron.feature.search.generated.resources.filter_from_anyone
import heron.feature.search.generated.resources.filter_from_following
import heron.feature.search.generated.resources.filter_include
import heron.feature.search.generated.resources.filter_include_all
import heron.feature.search.generated.resources.filter_include_posts
import heron.feature.search.generated.resources.filter_include_replies
import heron.feature.search.generated.resources.filter_language
import heron.feature.search.generated.resources.filter_language_all
import heron.feature.search.generated.resources.filter_media
import heron.feature.search.generated.resources.filter_media_all
import heron.feature.search.generated.resources.filter_media_videos
import heron.feature.search.generated.resources.filter_media_with_media
import heron.feature.search.generated.resources.filter_mode_exclude
import heron.feature.search.generated.resources.filter_mode_include
import heron.feature.search.generated.resources.filter_none_words
import heron.feature.search.generated.resources.filter_none_words_hint
import heron.feature.search.generated.resources.filter_people_authors
import heron.feature.search.generated.resources.filter_people_mentions
import heron.feature.search.generated.resources.filter_pick_date
import heron.feature.search.generated.resources.filter_remove
import heron.feature.search.generated.resources.filter_search
import heron.feature.search.generated.resources.filter_select_people
import heron.feature.search.generated.resources.filter_since
import heron.feature.search.generated.resources.filter_until
import heron.feature.search.generated.resources.filters_title
import heron.ui.core.generated.resources.done
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchFilterForm(
    paneScaffoldState: PaneScaffoldState,
    queryText: String,
    filter: SearchQuery.Filter,
    onQueryTextChanged: (String) -> Unit,
    onFilterChanged: (SearchQuery.Filter) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    // A single, shared profile picker services every person group; the group being
    // edited is tracked so its callback appends to the right row.
    var editingGroupIndex by remember { mutableStateOf<Int?>(null) }
    val profilePicker = paneScaffoldState.rememberProfileSearchSheetState { profile ->
        val index = editingGroupIndex ?: return@rememberProfileSearchSheetState
        onFilterChanged(
            filter.copy(
                people = filter.people.updatedAt(index) { group ->
                    group.copy(
                        profileIds = (group.profileIds + profile.did).distinct(),
                    )
                },
            ),
        )
    }
    LaunchedEffect(filter.people) {
        profilePicker.seedProfiles(
            filter.people
                .flatMap(SearchQuery.Filter.PersonGroup::profileIds),
        )
    }
    val peopleTitle = stringResource(Res.string.filter_select_people)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f),
    ) {
        FilterTopBar(
            onCancel = onCancel,
            onApply = onApply,
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = AllWordsKey) {
                LabeledTextField(
                    modifier = Modifier
                        .animateItem(),
                    label = stringResource(Res.string.filter_all_words),
                    value = queryText,
                    onValueChange = onQueryTextChanged,
                )
            }
            item(key = NoneAndPhraseKey) {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LabeledTextField(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_none_words),
                        value = filter.noneOfWords.orEmpty(),
                        placeholder = stringResource(Res.string.filter_none_words_hint),
                        onValueChange = {
                            onFilterChanged(filter.copy(noneOfWords = it.ifBlank { null }))
                        },
                    )
                    LabeledTextField(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_exact_phrase),
                        value = filter.exactPhrase.orEmpty(),
                        placeholder = stringResource(Res.string.filter_exact_phrase_hint),
                        onValueChange = {
                            onFilterChanged(filter.copy(exactPhrase = it.ifBlank { null }))
                        },
                    )
                }
            }
            item(key = DateRangeKey) {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DateField(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_since),
                        date = filter.since,
                        onDateChanged = { onFilterChanged(filter.copy(since = it)) },
                    )
                    DateField(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_until),
                        date = filter.until,
                        onDateChanged = { onFilterChanged(filter.copy(until = it)) },
                    )
                }
            }
            item(key = LanguageMediaKey) {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LanguageDropdown(
                        modifier = Modifier
                            .weight(1f),
                        language = filter.language,
                        onLanguageChanged = { onFilterChanged(filter.copy(language = it)) },
                    )
                    EnumDropdown(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_media),
                        options = SearchQuery.Filter.Media.entries,
                        selected = filter.media,
                        optionLabel = { stringResource(it.labelRes) },
                        onSelected = { onFilterChanged(filter.copy(media = it)) },
                    )
                }
            }
            item(key = IncludeFromKey) {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EnumDropdown(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_include),
                        options = SearchQuery.Filter.Replies.entries,
                        selected = filter.replies,
                        optionLabel = { stringResource(it.labelRes) },
                        onSelected = { onFilterChanged(filter.copy(replies = it)) },
                    )
                    EnumDropdown(
                        modifier = Modifier
                            .weight(1f),
                        label = stringResource(Res.string.filter_from),
                        options = SearchQuery.Filter.From.entries,
                        selected = filter.from,
                        optionLabel = { stringResource(it.labelRes) },
                        onSelected = { onFilterChanged(filter.copy(from = it)) },
                    )
                }
            }

            filter.people.forEachIndexed { index, group ->
                item(key = group.id) {
                    PersonGroupCard(
                        modifier = Modifier
                            .animateItem(),
                        group = group,
                        resolveHandle = { profileId ->
                            profilePicker.cachedProfiles
                                .firstOrNull { it.did == profileId }
                                ?.handle
                                ?.id
                                ?: profileId.id
                        },
                        onModeChanged = { mode ->
                            onFilterChanged(
                                filter.copy(people = filter.people.updatedAt(index) { it.copy(mode = mode) }),
                            )
                        },
                        onKindChanged = { kind ->
                            onFilterChanged(
                                filter.copy(people = filter.people.updatedAt(index) { it.copy(kind = kind) }),
                            )
                        },
                        onAddPeople = {
                            editingGroupIndex = index
                            profilePicker.show(title = peopleTitle)
                        },
                        onRemovePerson = { profileId ->
                            onFilterChanged(
                                filter.copy(
                                    people = filter.people.updatedAt(index) {
                                        it.copy(profileIds = it.profileIds - profileId)
                                    },
                                ),
                            )
                        },
                        onRemoveGroup = {
                            onFilterChanged(
                                filter.copy(
                                    people = filter.people.filterIndexed { i, _ -> i != index },
                                ),
                            )
                        },
                    )
                }
            }
            item(key = AddPeopleFilterKey) {
                FilledTonalButton(
                    onClick = {
                        onFilterChanged(
                            filter.copy(people = filter.people + SearchQuery.Filter.PersonGroup()),
                        )
                    },
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(text = stringResource(Res.string.filter_add_people_filter))
                }
            }
            item(key = BottomSpacerKey) {
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                )
            }
        }
    }
}

@Composable
private fun FilterTopBar(
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(Res.string.filter_cancel))
        }
        Text(
            text = stringResource(Res.string.filters_title),
            style = MaterialTheme.typography.titleLarge,
        )
        FilledTonalButton(
            onClick = onApply,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(Res.string.filter_search))
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = placeholder?.let { { Text(text = it) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onDateChanged: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = date?.toString() ?: stringResource(Res.string.filter_date_prompt),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        showPicker = true
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = stringResource(Res.string.filter_pick_date),
                        )
                    },
                )
            },
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toUtcEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChanged(pickerState.selectedDateMillis?.toUtcLocalDate())
                        showPicker = false
                    },
                ) {
                    Text(text = stringResource(CommonStrings.done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDateChanged(null)
                        showPicker = false
                    },
                ) {
                    Text(text = stringResource(Res.string.filter_clear_date))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun PersonGroupCard(
    modifier: Modifier = Modifier,
    group: SearchQuery.Filter.PersonGroup,
    resolveHandle: (ProfileId) -> String,
    onModeChanged: (SearchQuery.Filter.PersonGroup.Mode) -> Unit,
    onKindChanged: (SearchQuery.Filter.PersonGroup.Kind) -> Unit,
    onAddPeople: () -> Unit,
    onRemovePerson: (ProfileId) -> Unit,
    onRemoveGroup: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EnumDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.filter_include),
                    options = SearchQuery.Filter.PersonGroup.Mode.entries,
                    selected = group.mode,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelected = onModeChanged,
                )
                EnumDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.filter_from),
                    options = SearchQuery.Filter.PersonGroup.Kind.entries,
                    selected = group.kind,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelected = onKindChanged,
                )
                IconButton(onClick = onRemoveGroup) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.filter_remove),
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.profileIds.forEach { profileId ->
                    InputChip(
                        selected = false,
                        onClick = onAddPeople,
                        label = {
                            Text(
                                text = resolveHandle(profileId),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onRemovePerson(profileId) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
            }
            TextButton(
                onClick = onAddPeople,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.filter_add_people))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    language: String?,
    onLanguageChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(Res.string.filter_language_all)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = SearchLanguages.firstOrNull { it.code == language }?.displayName ?: allLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = stringResource(Res.string.filter_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = allLabel) },
                onClick = {
                    onLanguageChanged(null)
                    expanded = false
                },
            )
            SearchLanguages.forEach { searchLanguage ->
                DropdownMenuItem(
                    text = { Text(text = searchLanguage.displayName) },
                    onClick = {
                        onLanguageChanged(searchLanguage.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun <T> List<T>.updatedAt(
    index: Int,
    transform: (T) -> T,
): List<T> = mapIndexed { i, item ->
    if (i == index) transform(item) else item
}

private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date

private val SearchQuery.Filter.Media.labelRes: StringResource
    get() = when (this) {
        SearchQuery.Filter.Media.All -> Res.string.filter_media_all
        SearchQuery.Filter.Media.WithMedia -> Res.string.filter_media_with_media
        SearchQuery.Filter.Media.VideosOnly -> Res.string.filter_media_videos
    }

private val SearchQuery.Filter.Replies.labelRes: StringResource
    get() = when (this) {
        SearchQuery.Filter.Replies.PostsAndReplies -> Res.string.filter_include_all
        SearchQuery.Filter.Replies.PostsOnly -> Res.string.filter_include_posts
        SearchQuery.Filter.Replies.RepliesOnly -> Res.string.filter_include_replies
    }

private val SearchQuery.Filter.From.labelRes: StringResource
    get() = when (this) {
        SearchQuery.Filter.From.Anyone -> Res.string.filter_from_anyone
        SearchQuery.Filter.From.Following -> Res.string.filter_from_following
    }

private val SearchQuery.Filter.PersonGroup.Mode.labelRes: StringResource
    get() = when (this) {
        SearchQuery.Filter.PersonGroup.Mode.Include -> Res.string.filter_mode_include
        SearchQuery.Filter.PersonGroup.Mode.Exclude -> Res.string.filter_mode_exclude
    }

private val SearchQuery.Filter.PersonGroup.Kind.labelRes: StringResource
    get() = when (this) {
        SearchQuery.Filter.PersonGroup.Kind.Authors -> Res.string.filter_people_authors
        SearchQuery.Filter.PersonGroup.Kind.Mentions -> Res.string.filter_people_mentions
    }

private data class SearchLanguage(
    val code: String,
    val displayName: String,
)

private val SearchLanguages: List<SearchLanguage> = listOf(
    SearchLanguage("en", "English"),
    SearchLanguage("es", "Spanish"),
    SearchLanguage("pt", "Portuguese"),
    SearchLanguage("fr", "French"),
    SearchLanguage("de", "German"),
    SearchLanguage("it", "Italian"),
    SearchLanguage("nl", "Dutch"),
    SearchLanguage("ja", "Japanese"),
    SearchLanguage("ko", "Korean"),
    SearchLanguage("zh", "Chinese"),
    SearchLanguage("ar", "Arabic"),
    SearchLanguage("hi", "Hindi"),
    SearchLanguage("ru", "Russian"),
    SearchLanguage("tr", "Turkish"),
    SearchLanguage("uk", "Ukrainian"),
)

private const val AllWordsKey = "com.tunjid.heron.search.filter.all_words"
private const val NoneAndPhraseKey = "com.tunjid.heron.search.filter.none_and_phrase"
private const val DateRangeKey = "com.tunjid.heron.search.filter.date_range"
private const val LanguageMediaKey = "com.tunjid.heron.search.filter.language_media"
private const val IncludeFromKey = "com.tunjid.heron.search.filter.include_from"
private const val AddPeopleFilterKey = "com.tunjid.heron.search.filter.add_people_filter"
private const val BottomSpacerKey = "com.tunjid.heron.search.filter.bottom_spacer"
