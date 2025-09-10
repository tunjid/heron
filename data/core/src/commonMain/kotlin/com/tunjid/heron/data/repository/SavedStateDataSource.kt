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
import androidx.datastore.core.okio.OkioStorage
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.datastore.migrations.VersionedSavedState
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.Writable
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
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import sh.christian.ozone.api.model.JsonContent

@Serializable
abstract class SavedState {
//    abstract val auth: AuthTokens?
    abstract val navigation: Navigation
//    abstract val profileData: Map<ProfileId, ProfileData>
    abstract val signedInProfileData: ProfileData?

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
    data class Writes(
        val pendingWrites: List<Writable> = emptyList(),
        val failedWrites: List<FailedWrite> = emptyList(),
    )

    @Serializable
    data class ProfileData(
        val preferences: Preferences,
        val notifications: Notifications,
        // Need default for migration
        val writes: Writes = Writes(),
    )
}

private val GuestAuth = SavedState.AuthTokens(
    authProfileId = Constants.unknownAuthorId,
    auth = "",
    refresh = "",
    didDoc = SavedState.AuthTokens.DidDoc(),
)

val InitialSavedState: SavedState = VersionedSavedState.Initial

val EmptySavedState: SavedState = VersionedSavedState.Empty

//internal val SavedStateDataSource.signedInProfileId
//    get() = savedState
//        .value
//        .auth
//        .ifSignedIn()
//        ?.authProfileId

internal val SavedStateDataSource.signedInProfileId2: ProfileId?
    get() = (savedState.value as? VersionedSavedState)?.auth.ifSignedIn()?.authProfileId


//internal val SavedStateDataSource.observedSignedInProfileId
//    get() = savedState
//        .map { it.auth.ifSignedIn()?.authProfileId }
//        .distinctUntilChanged()

internal val SavedStateDataSource.observedSignedInProfileId2
    get() = savedState
        .map { (it as? VersionedSavedState)?.auth.ifSignedIn()?.authProfileId }
        .distinctUntilChanged()


//internal val SavedStateDataSource.signedInAuth
//    get() = savedState
//        .map { it.auth.ifSignedIn() }
//        .distinctUntilChanged()

internal val SavedStateDataSource.signedInAuth
    get() = savedState
        .map { (it as? VersionedSavedState)?.auth.ifSignedIn() }
        .distinctUntilChanged()

private fun SavedState.AuthTokens?.ifSignedIn() =
    this?.takeUnless(GuestAuth::equals)

//fun SavedState.signedProfilePreferencesOrDefault() =
//    auth.ifSignedIn()
//        ?.let { profileData[it.authProfileId] }
//        ?.preferences
//        ?: Preferences.DefaultPreferences

fun SavedState.signedProfilePreferencesOrDefault2() =
    signedInProfileData?.preferences ?: Preferences.DefaultPreferences


//fun SavedState.signedInProfileNotifications() =
//    auth.ifSignedIn()
//        ?.let { profileData[it.authProfileId] }
//        ?.notifications

fun SavedState.signedInProfileNotifications2() =
    signedInProfileData?.notifications


//internal val SavedState.signedInProfileId
//    get() = auth.ifSignedIn()?.authProfileId

internal val SavedState.signedInProfileId2: ProfileId?
    get() = (this as? VersionedSavedState)?.auth.ifSignedIn()?.authProfileId

internal val SavedState.signedInProfileId3: ProfileId?
    get() = signedInProfileData?.let { (this as? VersionedSavedState)?.auth?.authProfileId }



//fun SavedState.isSignedIn() =
//    auth.ifSignedIn() != null

fun SavedState.isSignedIn2() =
    signedInProfileData != null


internal suspend fun SavedStateDataSource.guestSignIn() =
    setAuth(auth = GuestAuth)

//fun SavedState.signedInUserPreferences() =
//    auth?.authProfileId
//        ?.let { profileData[it] }
//        ?.preferences

fun SavedState.signedInUserPreferences2() =
    signedInProfileData?.preferences


sealed class SavedStateDataSource {
    abstract val savedState: StateFlow<SavedState>

    abstract suspend fun setNavigationState(
        navigation: SavedState.Navigation,
    )

    internal abstract suspend fun setAuth(
        auth: SavedState.AuthTokens?,
    )

    internal abstract suspend fun updateSignedInProfileData(
        block: SavedState.ProfileData.(signedInProfileId: ProfileId?) -> SavedState.ProfileData,
    )

    abstract suspend fun setLastViewedHomeTimelineUri(uri: Uri)
}

@Inject
internal class DataStoreSavedStateDataSource(
    path: Path,
    fileSystem: FileSystem,
    @Named("AppScope") appScope: CoroutineScope,
    protoBuf: ProtoBuf,
) : SavedStateDataSource() {

    private val dataStore: DataStore<VersionedSavedState> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            serializer = VersionedSavedStateOkioSerializer(protoBuf),
            producePath = { path },
        ),
        scope = appScope,
    )

    override val savedState = dataStore.data.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = InitialSavedState,
    )

    override suspend fun setNavigationState(
        navigation: SavedState.Navigation,
    ) = updateState {
        copy(navigation = navigation)
    }

    override suspend fun setAuth(
        auth: SavedState.AuthTokens?,
    ) = updateState {
        copy(auth = auth)
    }

    override suspend fun updateSignedInProfileData(
        block: SavedState.ProfileData.(signedInProfileId: ProfileId?) -> SavedState.ProfileData,
    ) = updateState {
        val signedInProfileId = auth.ifSignedIn()?.authProfileId ?: return@updateState this
        val signedInProfileData = profileData[signedInProfileId] ?: SavedState.ProfileData(
            notifications = SavedState.Notifications(),
            preferences = Preferences.DefaultPreferences,
            writes = SavedState.Writes(),
        )
        val update = signedInProfileId to signedInProfileData.block(signedInProfileId)
        copy(
            profileData = profileData + update,
        )
    }

    override suspend fun setLastViewedHomeTimelineUri(uri: Uri) =
        updateSignedInProfileData {
            copy(preferences = preferences.copy(lastViewedHomeTimelineUri = uri))
        }

    private suspend fun updateState(update: VersionedSavedState.() -> VersionedSavedState) {
        dataStore.updateData(update)
    }
}

internal suspend fun SavedStateDataSource.updateSignedInUserPreferences(
    preferences: Preferences,
) = updateSignedInProfileData {
    copy(preferences = preferences)
}

internal suspend fun SavedStateDataSource.updateSignedInUserNotifications(
    block: SavedState.Notifications.() -> SavedState.Notifications,
) = updateSignedInProfileData {
    copy(notifications = notifications.block())
}
