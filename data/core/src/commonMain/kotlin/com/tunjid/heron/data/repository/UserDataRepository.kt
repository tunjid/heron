package com.tunjid.heron.data.repository

import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.PutPreferencesRequest
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface UserDataRepository {

    val preferences: Flow<Preferences>

    val navigation: Flow<SavedState.Navigation>

    suspend fun persistNavigationState(
        navigation: SavedState.Navigation,
    ): Outcome

    suspend fun setLastViewedHomeTimelineUri(
        uri: Uri,
    ): Outcome

    suspend fun setRefreshedHomeTimelineOnLaunch(
        refreshOnLaunch: Boolean,
    ): Outcome

    suspend fun setDynamicTheming(
        dynamicTheming: Boolean,
    ): Outcome

    suspend fun setCompactNavigation(
        compactNavigation: Boolean,
    ): Outcome

    suspend fun updateMutedWords(
        mutedWordPreferences: List<MutedWordPreference>,
    ): Outcome

    suspend fun refreshPreferences(): Outcome
}

internal class OfflineUserDataRepository @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
    private val preferenceUpdater: PreferenceUpdater,
    private val networkService: NetworkService,
) : UserDataRepository {

    override val preferences: Flow<Preferences> =
        savedStateDataSource.savedState
            .map {
                it.signedInProfileData
                    ?.preferences
                    ?: Preferences.EmptyPreferences
            }
            .distinctUntilChanged()

    override val navigation: Flow<SavedState.Navigation>
        get() = savedStateDataSource.savedState
            .map { it.navigation }

    override suspend fun persistNavigationState(
        navigation: SavedState.Navigation,
    ): Outcome = runCatchingUnlessCancelled {
        if (navigation != InitialNavigation) savedStateDataSource.setNavigationState(
            navigation = navigation,
        )
    }.toOutcome()

    override suspend fun setLastViewedHomeTimelineUri(
        uri: Uri,
    ): Outcome = updatePreferences {
        copy(lastViewedHomeTimelineUri = uri)
    }

    override suspend fun setRefreshedHomeTimelineOnLaunch(
        refreshOnLaunch: Boolean,
    ): Outcome = updatePreferences {
        copy(refreshHomeTimelineOnLaunch = refreshOnLaunch)
    }

    override suspend fun setDynamicTheming(
        dynamicTheming: Boolean,
    ): Outcome = updatePreferences {
        copy(useDynamicTheming = dynamicTheming)
    }

    override suspend fun setCompactNavigation(
        compactNavigation: Boolean,
    ): Outcome = updatePreferences {
        copy(useCompactNavigation = compactNavigation)
    }

    override suspend fun updateMutedWords(
        mutedWordPreferences: List<MutedWordPreference>,
    ): Outcome {
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            getPreferencesForActor()
        }.getOrElse { return Outcome.Failure(it) }

        val updatedPreferences = preferenceUpdater.update(
            response = response,
            update = Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = mutedWordPreferences,
            ),
        )

        return networkService.runCatchingWithMonitoredNetworkRetry {
            putPreferences(
                PutPreferencesRequest(
                    preferences = updatedPreferences,
                ),
            )
        }.fold(
            onSuccess = { refreshPreferences() },
            onFailure = Outcome::Failure,
        )
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

    private suspend fun updatePreferences(
        updater: Preferences.() -> Preferences,
    ): Outcome =
        runCatchingUnlessCancelled {
            savedStateDataSource.updateSignedInProfileData {
                copy(preferences = preferences.updater())
            }
        }.toOutcome()
}
