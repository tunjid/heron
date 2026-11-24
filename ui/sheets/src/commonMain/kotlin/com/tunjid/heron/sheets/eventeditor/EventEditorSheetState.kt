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

package com.tunjid.heron.sheets.eventeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.media.images.rememberUpdatedImageState
import com.tunjid.heron.media.picker.MediaType
import com.tunjid.heron.media.picker.rememberMediaPicker
import com.tunjid.heron.timeline.utilities.formatHour
import com.tunjid.heron.timeline.utilities.formatWeekdayDate
import com.tunjid.heron.ui.Chip
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Image
import com.tunjid.heron.ui.shapes.roundedRectangle
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.mutator.compose.produceState
import heron.ui.core.generated.resources.cancel
import heron.ui.core.generated.resources.done
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.event_add_thumbnail
import heron.ui.timeline.generated.resources.event_create
import heron.ui.timeline.generated.resources.event_description
import heron.ui.timeline.generated.resources.event_edit
import heron.ui.timeline.generated.resources.event_ends
import heron.ui.timeline.generated.resources.event_mode
import heron.ui.timeline.generated.resources.event_mode_hybrid
import heron.ui.timeline.generated.resources.event_mode_in_person
import heron.ui.timeline.generated.resources.event_mode_virtual
import heron.ui.timeline.generated.resources.event_name
import heron.ui.timeline.generated.resources.event_online_link
import heron.ui.timeline.generated.resources.event_pick_date
import heron.ui.timeline.generated.resources.event_starts
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class EventEditorSheetState(
    scope: BottomSheetScope,
    internal val stateHolder: EventEditorStateHolder,
) : BottomSheetState(scope) {

    fun createEvent() {
        stateHolder.accept(
            EventEditorAction.Initialize(event = null),
        )
        show()
    }

    fun editEvent(
        event: Event,
    ) {
        stateHolder.accept(
            EventEditorAction.Initialize(event = event),
        )
        show()
    }

    override fun onHidden() {
        stateHolder.accept(EventEditorAction.Reset)
    }

    companion object {
        @Composable
        fun rememberUpdatedEventEditorSheetState(
            stateHolder: EventEditorStateHolder,
            onEventSaved: (Event.Put) -> Unit,
        ): EventEditorSheetState {
            val state = rememberBottomSheetState(
                stateHolder = stateHolder,
                block = ::EventEditorSheetState,
            )

            EventEditorBottomSheet(
                state = state,
                onEventSaved = onEventSaved,
            )

            return state
        }
    }
}

@Composable
private fun EventEditorBottomSheet(
    state: EventEditorSheetState,
    onEventSaved: (Event.Put) -> Unit,
) {
    state.ModalBottomSheet {
        val editorState = state.stateHolder.produceState()
        val accept = state.stateHolder.accept

        LaunchedEffect(editorState.saved) {
            val saved = editorState.saved ?: return@LaunchedEffect
            onEventSaved(saved)
            state.hide()
        }

        val thumbnailPicker = rememberMediaPicker(
            mediaType = MediaType.Photo,
            maxItems = 1,
        ) { items ->
            items
                .filterIsInstance<RestrictedFile.Media.Photo>()
                .firstOrNull()
                ?.let { accept(EventEditorAction.Update.Thumbnail(it)) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (editorState.editingEvent == null) Res.string.event_create
                    else Res.string.event_edit,
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            EventThumbnail(
                thumbnail = editorState.thumbnail,
                existingThumbnailUrl = editorState.editingEvent?.thumbnail?.uri,
                onClick = thumbnailPicker,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = editorState.name,
                onValueChange = { accept(EventEditorAction.Update.Name(it)) },
                label = { Text(text = stringResource(Res.string.event_name)) },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = editorState.description,
                onValueChange = { accept(EventEditorAction.Update.Description(it)) },
                label = {
                    Text(text = stringResource(Res.string.event_description))
                },
                minLines = 2,
            )

            DateTimeRow(
                label = Res.string.event_starts,
                instant = editorState.startsAt,
                onInstantChanged = { accept(EventEditorAction.Update.StartsAt(it)) },
            )
            DateTimeRow(
                label = Res.string.event_ends,
                instant = editorState.endsAt,
                onInstantChanged = { accept(EventEditorAction.Update.EndsAt(it)) },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = stringResource(Res.string.event_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                EventModes.forEach { (eventMode, label) ->
                    Chip(
                        selected = editorState.mode == eventMode,
                        text = stringResource(label),
                        onClick = { accept(EventEditorAction.Update.Mode(eventMode)) },
                    )
                }
            }

            if (editorState.mode != Event.Mode.InPerson) OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = editorState.onlineLink,
                onValueChange = { accept(EventEditorAction.Update.OnlineLink(it)) },
                label = { Text(text = stringResource(Res.string.event_online_link)) },
                singleLine = true,
            )

            FilledTonalButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 16.dp),
                enabled = editorState.canSave && editorState.saved == null,
                onClick = { accept(EventEditorAction.Save) },
                content = {
                    Text(text = stringResource(CommonStrings.done))
                },
            )
        }
    }
}

@Composable
private fun EventThumbnail(
    thumbnail: RestrictedFile.Media.Photo?,
    existingThumbnailUrl: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            thumbnail != null -> AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                state = rememberUpdatedImageState(
                    args = ImageArgs(
                        item = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        shape = ThumbnailShape,
                    ),
                ),
            )
            existingThumbnailUrl != null -> AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                state = rememberUpdatedImageState(
                    args = ImageArgs(
                        url = existingThumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        shape = ThumbnailShape,
                    ),
                ),
            )
            else -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = HeronIcons.Regular.Image,
                    contentDescription = null,
                )
                Text(text = stringResource(Res.string.event_add_thumbnail))
            }
        }
    }
}

@Composable
private fun DateTimeRow(
    label: StringResource,
    instant: Instant?,
    onInstantChanged: (Instant?) -> Unit,
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val current = instant?.toLocalDateTime(timeZone)
    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerDate by remember { mutableStateOf<LocalDate?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
        )
        Chip(
            selected = instant != null,
            text = instant?.formatWeekdayDate() ?: stringResource(Res.string.event_pick_date),
            onClick = { showDatePicker = true },
        )
        if (instant != null && current != null) Chip(
            selected = true,
            text = instant.formatHour(),
            onClick = { timePickerDate = current.date },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (instant ?: Clock.System.now()).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        // The date picker reports UTC midnight of the picked day.
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                        if (current == null) timePickerDate = date
                        else onInstantChanged(
                            LocalDateTime(
                                date = date,
                                time = current.time,
                            ).toInstant(timeZone),
                        )
                    },
                    content = { Text(text = stringResource(CommonStrings.done)) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onInstantChanged(null)
                        showDatePicker = false
                    },
                    content = { Text(text = stringResource(CommonStrings.cancel)) },
                )
            },
            content = { DatePicker(state = datePickerState) },
        )
    }

    val date = timePickerDate
    if (date != null) {
        val timePickerState = rememberTimePickerState(
            initialHour = current?.hour ?: 18,
            initialMinute = current?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { timePickerDate = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        onInstantChanged(
                            LocalDateTime(
                                date = date,
                                time = LocalTime(
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute,
                                ),
                            ).toInstant(timeZone),
                        )
                        timePickerDate = null
                    },
                    content = { Text(text = stringResource(CommonStrings.done)) },
                )
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private val ThumbnailShape = PolygonShape.roundedRectangle(0.08f)

private val EventModes = listOf(
    Event.Mode.InPerson to Res.string.event_mode_in_person,
    Event.Mode.Virtual to Res.string.event_mode_virtual,
    Event.Mode.Hybrid to Res.string.event_mode_hybrid,
)
