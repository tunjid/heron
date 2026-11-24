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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.details
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.datetime.TimeZone

@Stable
interface EventEditorStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<EventEditorAction, EventEditorState>

@AssistedFactory
fun interface EventEditorViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): EventEditorViewModel
}

class EventEditorViewModel(
    mutator: ActionSuspendingStateMutator<EventEditorAction, EventEditorState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    EventEditorStateHolder,
    ActionSuspendingStateMutator<EventEditorAction, EventEditorState> by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        fileManager: FileManager,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = EventEditorState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                authRepository.signedInUser.launchedCollect {
                    state.signedInProfileId = it?.did
                }
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = EventEditorAction::key,
                ) {
                    when (val action = type()) {
                        is EventEditorAction.Initialize -> action.flow.launchInitializeMutations(
                            state = state,
                        )
                        is EventEditorAction.Update -> action.flow.launchUpdateMutations(
                            state = state,
                        )
                        is EventEditorAction.Save -> action.flow.launchSaveMutations(
                            state = state,
                            fileManager = fileManager,
                        )
                        is EventEditorAction.Reset -> action.flow.launchResetMutations(
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
private fun Flow<EventEditorAction.Initialize>.launchInitializeMutations(
    state: EventEditorState.Mutable,
) = launchedCollect { action ->
    val details = action.event?.details
    state.editingEvent = action.event
    state.name = details?.name.orEmpty()
    state.description = details?.description.orEmpty()
    state.startsAt = details?.startsAt
    state.endsAt = details?.endsAt
    state.mode = details?.mode?.takeIf { it != Event.Mode.Unknown } ?: Event.Mode.InPerson
    state.onlineLink = details?.onlineLink?.uri.orEmpty()
    state.thumbnail = null
    state.saved = null
}

context(productionScope: CoroutineScope)
private fun Flow<EventEditorAction.Update>.launchUpdateMutations(
    state: EventEditorState.Mutable,
) = launchedCollect { action ->
    when (action) {
        is EventEditorAction.Update.Name -> state.name = action.name
        is EventEditorAction.Update.Description -> state.description = action.description
        is EventEditorAction.Update.StartsAt -> state.startsAt = action.startsAt
        is EventEditorAction.Update.EndsAt -> state.endsAt = action.endsAt
        is EventEditorAction.Update.Mode -> state.mode = action.mode
        is EventEditorAction.Update.OnlineLink -> state.onlineLink = action.onlineLink
        is EventEditorAction.Update.Thumbnail -> state.thumbnail = action.thumbnail
    }
}

context(productionScope: CoroutineScope)
private fun Flow<EventEditorAction.Save>.launchSaveMutations(
    state: EventEditorState.Mutable,
    fileManager: FileManager,
) = launchedCollect {
    // A repeated tap must not save twice: a second create would generate a second event.
    if (state.saved != null || !state.canSave) return@launchedCollect
    val signedInProfileId = state.signedInProfileId ?: return@launchedCollect

    val editingEvent = state.editingEvent
    val details = Event.Details(
        name = state.name.trim(),
        description = state.description.trim().ifBlank { null },
        startsAt = state.startsAt,
        endsAt = state.endsAt,
        mode = state.mode,
        onlineLink = state.onlineLink
            .trim()
            .takeIf { state.mode != Event.Mode.InPerson && it.isNotBlank() }
            ?.let(::GenericUri),
        // New events record the timezone they were planned in. Edits keep whatever the event
        // already declared.
        timezone =
        if (editingEvent == null) TimeZone.currentSystemDefault().id
        else null,
    )
    val thumbnail = state.thumbnail
        ?.let {
            fileManager.cacheWithoutRestrictions(
                restrictedFile = it,
                maxSizeInBytes = MaxEventThumbnailSizeInBytes,
            )
        }
        as? File.Media.Photo

    state.saved = when (editingEvent) {
        null -> Event.Create(
            uri = Event.uri(
                authorId = signedInProfileId,
                recordKey = RecordKey.generate(),
            ),
            details = details,
            thumbnail = thumbnail,
        )
        else -> Event.Edit(
            uri = editingEvent.uri,
            details = details,
            thumbnail = thumbnail,
        )
    }
}

context(productionScope: CoroutineScope)
private fun Flow<EventEditorAction.Reset>.launchResetMutations(
    state: EventEditorState.Mutable,
) = launchedCollect {
    state.editingEvent = null
    state.thumbnail = null
    state.saved = null
}

@Stable
@Snapshottable
interface EventEditorState {
    @SnapshotSpec
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        val editingEvent: Event? = null,
        val name: String = "",
        val description: String = "",
        val startsAt: Instant? = null,
        val endsAt: Instant? = null,
        val mode: Event.Mode = Event.Mode.InPerson,
        val onlineLink: String = "",
        val thumbnail: RestrictedFile.Media.Photo? = null,
        // The finished write, ready for the caller. Set once per session of the sheet.
        val saved: Event.Put? = null,
    ) : EventEditorState
}

val EventEditorState.canSave: Boolean
    get() {
        val start = startsAt ?: return false
        val end = endsAt
        return name.isNotBlank() && (end == null || end >= start)
    }

sealed class EventEditorAction(val key: String) {
    data class Initialize(
        val event: Event?,
    ) : EventEditorAction("Initialize")

    sealed class Update : EventEditorAction("Update") {
        data class Name(
            val name: String,
        ) : Update()

        data class Description(
            val description: String,
        ) : Update()

        data class StartsAt(
            val startsAt: Instant?,
        ) : Update()

        data class EndsAt(
            val endsAt: Instant?,
        ) : Update()

        data class Mode(
            val mode: Event.Mode,
        ) : Update()

        data class OnlineLink(
            val onlineLink: String,
        ) : Update()

        data class Thumbnail(
            val thumbnail: RestrictedFile.Media.Photo,
        ) : Update()
    }

    data object Save : EventEditorAction("Save")

    data object Reset : EventEditorAction("Reset")
}

// The blob limit for media in community.lexicon.calendar.event.
private const val MaxEventThumbnailSizeInBytes = 1_000_000L
