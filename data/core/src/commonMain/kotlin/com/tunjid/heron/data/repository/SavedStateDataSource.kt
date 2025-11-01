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
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.datastore.migrations.VersionedSavedState
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.Writable
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import sh.christian.ozone.api.model.JsonContent

@Serializable
abstract class SavedState {
    abstract val auth: AuthTokens?
    abstract val navigation: Navigation
    abstract val signedInProfileData: ProfileData?

    @Serializable
    sealed class AuthTokens {
        abstract val authProfileId: ProfileId

        @Serializable
        data class Guest(
            val server: Server,
        ) : AuthTokens() {
            override val authProfileId: ProfileId = Constants.unknownAuthorId
        }

        @Serializable
        sealed class Authenticated : AuthTokens() {

            internal val serviceUrl: String?
                get() = when (this) {
                    is Bearer ->
                        didDoc.service
                            .firstOrNull()
                            ?.serviceEndpoint
                    is DPoP -> pdsUrl
                }

            @Serializable
            data class Bearer(
                override val authProfileId: ProfileId,
                val auth: String,
                val refresh: String,
                val didDoc: DidDoc = DidDoc(),
                val authEndpoint: String,
            ) : Authenticated()

            @Serializable
            data class DPoP(
                override val authProfileId: ProfileId,
                val auth: String,
                val refresh: String,
                val pdsUrl: String,
                val clientId: String,
                val nonce: String,
                val keyPair: DERKeyPair,
                val issuerEndpoint: String,
            ) : Authenticated() {
                @Serializable
                class DERKeyPair(
                    val publicKey: ByteArray,
                    val privateKey: ByteArray,
                )
            }
        }

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

val InitialSavedState: SavedState = VersionedSavedState.Initial

val EmptySavedState: SavedState = VersionedSavedState.Empty

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

private fun SavedState.AuthTokens?.ifSignedIn(): SavedState.AuthTokens.Authenticated? =
    when (this) {
        is SavedState.AuthTokens.Authenticated -> this
        is SavedState.AuthTokens.Guest,
        null,
        -> null
    }

internal fun SavedState.signedProfilePreferencesOrDefault(): Preferences =
    signedInProfileData
        ?.preferences
        ?: when (val authTokens = auth) {
            is SavedState.AuthTokens.Authenticated.Bearer -> authTokens.authEndpoint
            is SavedState.AuthTokens.Authenticated.DPoP -> authTokens.issuerEndpoint
            is SavedState.AuthTokens.Guest -> authTokens.server.endpoint
            null -> Server.BlueSky.endpoint
        }.let(::preferencesForUrl)

internal val SavedState.signedInProfileId: ProfileId?
    get() = auth.ifSignedIn()?.authProfileId

fun SavedState.isSignedIn(): Boolean =
    auth.ifSignedIn() != null

fun SavedState.signedInProfilePreferences() =
    signedInProfileData?.preferences

private fun preferencesForUrl(url: String) =
    when (url) {
        Server.BlueSky.endpoint -> Preferences.BlueSkyGuestPreferences
        Server.BlackSky.endpoint -> Preferences.BlackSkyGuestPreferences
        else -> Preferences.BlueSkyGuestPreferences
    }

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

    abstract suspend fun setLastViewedHomeTimelineUri(
        uri: Uri,
    )

    abstract suspend fun setRefreshedHomeTimelineOnLaunch(
        refreshOnLaunch: Boolean,
    )
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
            preferences = Preferences.EmptyPreferences,
            writes = SavedState.Writes(),
        )
        val update = signedInProfileId to signedInProfileData.block(signedInProfileId)
        copy(
            profileData = profileData + update,
        )
    }

    override suspend fun setLastViewedHomeTimelineUri(
        uri: Uri,
    ) = updateSignedInProfileData {
        copy(preferences = preferences.copy(lastViewedHomeTimelineUri = uri))
    }

    override suspend fun setRefreshedHomeTimelineOnLaunch(
        refreshOnLaunch: Boolean,
    ) = updateSignedInProfileData {
        copy(preferences = preferences.copy(refreshHomeTimelineOnLaunch = refreshOnLaunch))
    }

    private suspend fun updateState(update: VersionedSavedState.() -> VersionedSavedState) {
        dataStore.updateData(update)
    }
}

internal suspend fun SavedStateDataSource.updateSignedInUserNotifications(
    block: SavedState.Notifications.() -> SavedState.Notifications,
) = updateSignedInProfileData {
    copy(notifications = notifications.block())
}

/**
 * Runs the [block] in the context of a single profile's session
 */
internal suspend inline fun <T> SavedStateDataSource.inCurrentProfileSession(
    crossinline block: suspend (ProfileId?) -> T,
): T? {
    val currentProfileId = savedState.value.signedInProfileId ?: return null
    return coroutineScope {
        select {
            async {
                savedState.first { it.signedInProfileId != currentProfileId }
                null
            }.onAwait { it }
            async {
                block(currentProfileId)
            }.onAwait { it }
        }.also { coroutineContext.cancelChildren() }
    }
}

/**
 * Repeats [block] for each signed in user
 */
internal suspend inline fun SavedStateDataSource.onEachSignedInProfile(
    crossinline block: suspend () -> Unit,
) = observedSignedInProfileId.collectLatest { profileId ->
    if (profileId != null) block()
}

/**
 * Helper extension method when handling [inCurrentProfileSession] in flow*/
internal inline fun <T> SavedStateDataSource.currentSessionFlow(
    crossinline block: suspend (ProfileId?) -> Flow<T>,
): Flow<T> = flow {
    inCurrentProfileSession { signedInProfileId ->
        emitAll(block(signedInProfileId))
    }
}
