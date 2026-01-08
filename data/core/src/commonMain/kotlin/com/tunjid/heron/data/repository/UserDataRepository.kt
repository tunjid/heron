package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.utilities.Outcome
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
}

internal class OfflineUserDataRepository @Inject constructor(
    private val savedStateDataSource: SavedStateDataSource,
) : UserDataRepository {

    override val preferences: Flow<Preferences> =
        savedStateDataSource.savedState
            .map(SavedState::signedProfilePreferencesOrDefault)
            .distinctUntilChanged()

    override val navigation: Flow<SavedState.Navigation>
        get() = savedStateDataSource.savedState
            .map { it.navigation }
            .distinctUntilChanged()

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

    private suspend inline fun updatePreferences(
        crossinline updater: suspend Preferences.() -> Preferences,
    ): Outcome =
        runCatchingUnlessCancelled {
            savedStateDataSource.updateSignedInProfileData {
                copy(preferences = preferences.updater())
            }
        }.toOutcome()
}

fun UserDataRepository.readPreferences(): Flow<Preferences> = preferences
