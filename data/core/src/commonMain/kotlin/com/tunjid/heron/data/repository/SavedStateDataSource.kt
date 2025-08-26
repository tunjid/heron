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
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.ProfileId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import sh.christian.ozone.api.model.JsonContent

@Serializable
data class SavedState(
    val auth: AuthTokens?,
    val navigation: Navigation,
    val profileData: Map<ProfileId, ProfileData>,
) {

    @Serializable
    data class AuthTokens(
        val authProfileId: ProfileId,
        val auth: String,
        val refresh: String,
        val didDoc: DidDoc = DidDoc(),
    ) {
        @Serializable
        data class DidDoc(
            val verificationMethod: List<VerificationMethod> = emptyList(),
            val service: List<Service> = emptyList(),
        ) {
            @Serializable
            data class VerificationMethod(
                val id: String,
                val type: String,
                val controller: String,
                val publicKeyMultibase: String,
            )

            @Serializable
            data class Service(
                val id: String,
                val type: String,
                val serviceEndpoint: String,
            )

            companion object {
                fun fromJsonContentOrEmpty(jsonContent: JsonContent?): DidDoc =
                    try {
                        jsonContent?.decodeAs<DidDoc>()
                    } catch (_: Exception) {
                        null
                    }
                        ?: DidDoc()
            }
        }
    }

    @Serializable
    data class Navigation(
        val activeNav: Int = 0,
        val backStacks: List<List<String>> = emptyList(),
    )

    @Serializable
    data class Notifications(
        val lastRead: Instant? = null,
        val lastRefreshed: Instant? = null,
    )

    @Serializable
    data class ProfileData(
        val preferences: Preferences,
        val notifications: Notifications,
    )
}

private val GuestAuth = SavedState.AuthTokens(
    authProfileId = Constants.unknownAuthorId,
    auth = "",
    refresh = "",
    didDoc = SavedState.AuthTokens.DidDoc(),
)

val InitialSavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = -1),
    profileData = emptyMap(),
)

val EmptySavedState = SavedState(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 0),
    profileData = emptyMap(),
)

internal val SavedStateDataSource.signedInProfileId
    get() = savedState
        .value
        .auth
        .ifSignedIn()
        ?.authProfileId

internal val SavedStateDataSource.observedSignedInProfileId
    get() = savedState
        .map { it.auth.ifSignedIn()?.authProfileId }
        .distinctUntilChanged()

internal val SavedStateDataSource.signedInAuth
    get() = savedState
        .map { it.auth.ifSignedIn() }
        .distinctUntilChanged()

private fun SavedState.AuthTokens?.ifSignedIn() =
    this?.takeUnless(GuestAuth::equals)

fun SavedState.signedProfilePreferencesOrDefault() =
    auth.ifSignedIn()
        ?.let { profileData[it.authProfileId] }
        ?.preferences
        ?: Preferences.DefaultPreferences

fun SavedState.signedInProfileNotifications() =
    auth.ifSignedIn()
        ?.let { profileData[it.authProfileId] }
        ?.notifications

internal val SavedState.signedInProfileId get() =
    auth.ifSignedIn()?.authProfileId
fun SavedState.isSignedIn() =
    auth.ifSignedIn() != null

internal suspend fun SavedStateDataSource.guestSignIn() =
    updateState { copy(auth = GuestAuth) }

interface SavedStateDataSource {
    val savedState: StateFlow<SavedState>
    suspend fun updateState(update: SavedState.() -> SavedState)
}

@Inject
internal class DataStoreSavedStateDataSource(
    path: Path,
    fileSystem: FileSystem,
    @Named("AppScope") appScope: CoroutineScope,
    protoBuf: ProtoBuf,
) : SavedStateDataSource {

    private val dataStore: DataStore<SavedState> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            serializer = SavedStateOkioSerializer(protoBuf),
            producePath = { path },
        ),
        scope = appScope,
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

internal suspend inline fun SavedStateDataSource.updateSignedInUserPreferences(
    preferences: Preferences,
) {
    updateSignedInProfileData {
        copy(preferences = preferences)
    }
}

internal suspend inline fun SavedStateDataSource.updateSignedInUserNotifications(
    crossinline block: SavedState.Notifications.() -> SavedState.Notifications,
) {
    updateSignedInProfileData {
        copy(notifications = notifications.block())
    }
}

private suspend inline fun SavedStateDataSource.updateSignedInProfileData(
    crossinline block: SavedState.ProfileData.() -> SavedState.ProfileData,
) {
    updateState {
        val signedInProfileId = auth.ifSignedIn()?.authProfileId ?: return@updateState this
        val signedInProfileData = profileData[signedInProfileId] ?: SavedState.ProfileData(
            notifications = SavedState.Notifications(),
            preferences = Preferences.DefaultPreferences,
        )
        copy(
            profileData = profileData + (signedInProfileId to signedInProfileData.block()),
        )
    }
}
