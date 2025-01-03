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
}

val InitialSavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = -1),
    preferences = null,
)

val EmptySavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 0),
    preferences = null,
)

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