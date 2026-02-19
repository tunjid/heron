package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.models.NotificationPreferences
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

    val notificationPreferences: Flow<NotificationPreferences>

    val preferences: Flow<Preferences>

    val navigation: Flow<SavedState.Navigation>

    suspend fun persistNavigationState(navigation: SavedState.Navigation): Outcome

    suspend fun setLastViewedHomeTimelineUri(uri: Uri): Outcome

    suspend fun setRefreshedHomeTimelineOnLaunch(refreshOnLaunch: Boolean): Outcome

    suspend fun setDynamicTheming(dynamicTheming: Boolean): Outcome

    suspend fun setCompactNavigation(compactNavigation: Boolean): Outcome

    suspend fun setAutoHideBottomNavigation(autoHideBottomNavigation: Boolean): Outcome

    suspend fun setAutoPlayTimelineVideos(autoPlayTimelineVideos: Boolean): Outcome
}

internal class OfflineUserDataRepository
@Inject
constructor(private val savedStateDataSource: SavedStateDataSource) : UserDataRepository {

    override val notificationPreferences: Flow<NotificationPreferences> =
        savedStateDataSource.savedState
            .map(SavedState::signedNotificationPreferencesOrDefault)
            .distinctUntilChanged()

    override val preferences: Flow<Preferences> =
        savedStateDataSource.savedState
            .map(SavedState::signedProfilePreferencesOrDefault)
            .distinctUntilChanged()

    override val navigation: Flow<SavedState.Navigation>
        get() = savedStateDataSource.savedState.map { it.navigation }.distinctUntilChanged()

    override suspend fun persistNavigationState(navigation: SavedState.Navigation): Outcome =
        runCatchingUnlessCancelled {
                if (navigation != InitialNavigation)
                    savedStateDataSource.setNavigationState(navigation = navigation)
            }
            .toOutcome()

    override suspend fun setLastViewedHomeTimelineUri(uri: Uri): Outcome = updatePreferences {
        copy(local = local.copy(lastViewedHomeTimelineUri = uri))
    }

    override suspend fun setRefreshedHomeTimelineOnLaunch(refreshOnLaunch: Boolean): Outcome =
        updatePreferences {
            copy(local = local.copy(refreshHomeTimelineOnLaunch = refreshOnLaunch))
        }

    override suspend fun setDynamicTheming(dynamicTheming: Boolean): Outcome = updatePreferences {
        copy(local = local.copy(useDynamicTheming = dynamicTheming))
    }

    override suspend fun setCompactNavigation(compactNavigation: Boolean): Outcome =
        updatePreferences {
            copy(local = local.copy(useCompactNavigation = compactNavigation))
        }

    override suspend fun setAutoHideBottomNavigation(autoHideBottomNavigation: Boolean): Outcome =
        updatePreferences {
            copy(local = local.copy(autoHideBottomNavigation = autoHideBottomNavigation))
        }

    override suspend fun setAutoPlayTimelineVideos(autoPlayTimelineVideos: Boolean): Outcome =
        updatePreferences {
            copy(local = local.copy(autoPlayTimelineVideos = autoPlayTimelineVideos))
        }

    private suspend inline fun updatePreferences(
        crossinline updater: suspend Preferences.() -> Preferences
    ): Outcome =
        runCatchingUnlessCancelled {
                savedStateDataSource.updateSignedInProfileData {
                    copy(preferences = preferences.updater())
                }
            }
            .toOutcome()
}
