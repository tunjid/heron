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

package com.tunjid.heron.data.repository


import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.Id
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import me.tatarka.inject.annotations.Inject
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path


@Serializable
data class SavedState(
    val auth: AuthTokens?,
    val navigation: Navigation,
    val preferences: Preferences?,
    val notifications: Notifications?,
) {

    @Serializable
    data class AuthTokens(
        val authProfileId: Id,
        val auth: String,
        val refresh: String,
    )

    @Serializable
    data class Navigation(
        val activeNav: Int = 0,
        val backStacks: List<List<String>> = emptyList(),
    )

    @Serializable
    data class Notifications(
        val lastSeen: Instant? = null,
    )
}

val InitialSavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = -1),
    preferences = null,
    notifications = null
)

val EmptySavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 0),
    preferences = null,
    notifications = null,
)

val SavedStateRepository.signedInProfileId
    get() = savedState
        .value
        .auth
        ?.authProfileId

interface SavedStateRepository {
    val savedState: StateFlow<SavedState>
    suspend fun updateState(update: SavedState.() -> SavedState)
}

@Inject
class DataStoreSavedStateRepository(
    path: Path,
    fileSystem: FileSystem,
    appScope: CoroutineScope,
    protoBuf: ProtoBuf,
) : SavedStateRepository {

    private val dataStore: DataStore<SavedState> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            serializer = SavedStateOkioSerializer(protoBuf),
            producePath = { path }
        ),
        scope = appScope
    )

    override val savedState = dataStore.data.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = InitialSavedState,
    )

    override suspend fun updateState(update: SavedState.() -> SavedState) {
        dataStore.updateData(update)
    }
}

private class SavedStateOkioSerializer(
    private val protoBuf: ProtoBuf,
) : OkioSerializer<SavedState> {
    override val defaultValue: SavedState = EmptySavedState

    override suspend fun readFrom(source: BufferedSource): SavedState =
        protoBuf.decodeFromByteArray(source.readByteArray())

    override suspend fun writeTo(t: SavedState, sink: BufferedSink) {
        sink.write(protoBuf.encodeToByteArray(value = t))
    }
}