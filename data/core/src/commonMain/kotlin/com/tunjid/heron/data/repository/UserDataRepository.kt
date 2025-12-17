package com.tunjid.heron.data.repository

import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.PreferencesDao
import com.tunjid.heron.data.database.entities.preferences.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

interface UserDataRepository {
    fun mutedWords(): Flow<List<MutedWordPreference>>

    suspend fun createMutedWord(
        preference: MutedWordPreference,
    ): Outcome

    suspend fun removeMutedWord(
        value: String,
    ): Outcome

    suspend fun clearAllMutedWords(): Outcome

    suspend fun refreshPreferences(): Outcome
}

internal class OfflineUserDataRepository @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
    private val preferenceUpdater: PreferenceUpdater,
    private val preferenceDao: PreferencesDao,
    private val networkService: NetworkService,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
) : UserDataRepository {

    override fun mutedWords(): Flow<List<MutedWordPreference>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            if (signedInProfileId == null) {
                emptyFlow()
            } else {
                preferenceDao.mutedWords(signedInProfileId)
                    .map { entities -> entities.map { it.asExternalModel() } }
                    .distinctUntilChanged()
            }
        }

    override suspend fun createMutedWord(preference: MutedWordPreference): Outcome {
        // Create update with the preference model
        val update = Timeline.Update.OfMutedWord.Add(preference = preference)

        return networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.fold(
            onSuccess = { preferencesResponse ->
                networkService.runCatchingWithMonitoredNetworkRetry {
                    putPreferences(
                        PutPreferencesRequest(
                            preferences = preferenceUpdater.update(
                                response = preferencesResponse,
                                update = update,
                            ),
                        ),
                    )
                }.fold(
                    onSuccess = {
                        updateLocalCache()
                        Outcome.Success
                    },
                    onFailure = Outcome::Failure,
                )
            },
            onFailure = Outcome::Failure,
        )
    }

    override suspend fun removeMutedWord(value: String): Outcome {
        val update = Timeline.Update.OfMutedWord.Remove(value = value)

        return networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.fold(
            onSuccess = { preferencesResponse ->
                networkService.runCatchingWithMonitoredNetworkRetry {
                    putPreferences(
                        PutPreferencesRequest(
                            preferences = preferenceUpdater.update(
                                response = preferencesResponse,
                                update = update,
                            ),
                        ),
                    )
                }.fold(
                    onSuccess = {
                        updateLocalCache()
                        Outcome.Success
                    },
                    onFailure = Outcome::Failure,
                )
            },
            onFailure = Outcome::Failure,
        )
    }

    override suspend fun clearAllMutedWords(): Outcome {
        val update = Timeline.Update.OfMutedWord.ClearAll
        return networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.fold(
            onSuccess = { preferencesResponse ->
                networkService.runCatchingWithMonitoredNetworkRetry {
                    putPreferences(
                        PutPreferencesRequest(
                            preferences = preferenceUpdater.update(
                                response = preferencesResponse,
                                update = update,
                            ),
                        ),
                    )
                }.fold(
                    onSuccess = {
                        updateLocalCache()
                        Outcome.Success
                    },
                    onFailure = Outcome::Failure,
                )
            },
            onFailure = Outcome::Failure,
        )
    }

    private suspend fun updateLocalCache() {
        // Fetch updated preferences and save to local DB
        networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.onSuccess { response ->
            savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
                signedInProfileId ?: return@inCurrentProfileSession

                multipleEntitySaverProvider.saveInTransaction {
                    // Save muted words from response
                    response.preferences.forEach { preference ->
                        if (preference is PreferencesUnion.MutedWordsPref) {
                            add(
                                viewingProfileId = signedInProfileId,
                                mutedWordsPref = preference,
                            )
                        }
                    }
                }
            }
        }
    }

    // Add refresh function
    override suspend fun refreshPreferences(): Outcome {
        return updateLocalCache().let {
            Outcome.Success
        }
    }
}
