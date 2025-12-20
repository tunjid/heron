package com.tunjid.heron.data.repository

import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.PutPreferencesRequest
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface UserDataRepository {
    fun mutedWords(): Flow<List<MutedWordPreference>>

    suspend fun createMutedWord(preference: MutedWordPreference): Outcome

    suspend fun removeMutedWord(value: String): Outcome

    suspend fun clearAllMutedWords(): Outcome

    suspend fun refreshPreferences(): Outcome
}

internal class OfflineUserDataRepository @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
    private val preferenceUpdater: PreferenceUpdater,
    private val networkService: NetworkService,
) : UserDataRepository {

    override fun mutedWords(): Flow<List<MutedWordPreference>> =
        savedStateDataSource.savedState
            .map {
                it.signedInProfileData
                    ?.preferences
                    ?.mutedWordPreferences
                    ?: emptyList()
            }
            .distinctUntilChanged()

    override suspend fun createMutedWord(preference: MutedWordPreference): Outcome {
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.getOrElse { return Outcome.Failure(it) }

        val update = Timeline.Update.OfMutedWord.Add(preference = preference)

        val putResult = networkService.runCatchingWithMonitoredNetworkRetry {
            putPreferences(
                PutPreferencesRequest(
                    preferences = preferenceUpdater.update(
                        response = response,
                        update = update,
                    ),
                ),
            )
        }
        if (putResult.isFailure) return Outcome.Failure(putResult.exceptionOrNull()!!)

        updatePreferences(response)
        return Outcome.Success
    }

    override suspend fun removeMutedWord(value: String): Outcome {
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.getOrElse { return Outcome.Failure(it) }

        val update = Timeline.Update.OfMutedWord.Remove(value = value)

        val putResult = networkService.runCatchingWithMonitoredNetworkRetry {
            putPreferences(
                PutPreferencesRequest(
                    preferences = preferenceUpdater.update(
                        response = response,
                        update = update,
                    ),
                ),
            )
        }
        if (putResult.isFailure) return Outcome.Failure(putResult.exceptionOrNull()!!)

        updatePreferences(response)
        return Outcome.Success
    }

    override suspend fun clearAllMutedWords(): Outcome {
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.getOrElse { return Outcome.Failure(it) }

        val update = Timeline.Update.OfMutedWord.ClearAll

        val putResult = networkService.runCatchingWithMonitoredNetworkRetry {
            putPreferences(
                PutPreferencesRequest(
                    preferences = preferenceUpdater.update(
                        response = response,
                        update = update,
                    ),
                ),
            )
        }
        if (putResult.isFailure) return Outcome.Failure(putResult.exceptionOrNull()!!)

        updatePreferences(response)
        return Outcome.Success
    }

    override suspend fun refreshPreferences(): Outcome {
        return networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.fold(
            onSuccess = { response ->
                updatePreferences(response)
                Outcome.Success
            },
            onFailure = Outcome::Failure,
        )
    }

    private suspend fun updatePreferences(response: GetPreferencesResponse) {
        val currentPreferences = savedStateDataSource.savedState.value
            .signedInProfileData
            ?.preferences
            ?: Preferences.EmptyPreferences

        val updatedPreferences = preferenceUpdater.update(
            response = response,
            preferences = currentPreferences,
        )

        savedStateDataSource.updateSignedInProfileData {
            copy(preferences = updatedPreferences)
        }
    }

    private fun updateRemoteMutedWords(){}
}
