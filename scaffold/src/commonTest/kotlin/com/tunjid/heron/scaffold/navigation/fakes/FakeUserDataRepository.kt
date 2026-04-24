package com.tunjid.heron.scaffold.navigation.fakes

import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserDataRepository : UserDataRepository {

    val navigationState = MutableStateFlow(SavedState.Navigation())
    val preferencesState = MutableStateFlow(Preferences.BlueSkyGuestPreferences)

    override val notificationPreferences: Flow<NotificationPreferences> =
        MutableStateFlow(NotificationPreferences.Default)

    override val preferences: Flow<Preferences> = preferencesState

    override val navigation: Flow<SavedState.Navigation> = navigationState

    override suspend fun persistNavigationState(navigation: SavedState.Navigation): Outcome =
        Outcome.Success

    override suspend fun setLastViewedHomeTimelineUri(uri: Uri): Outcome = Outcome.Success

    override suspend fun setRefreshedHomeTimelineOnLaunch(refreshOnLaunch: Boolean): Outcome =
        Outcome.Success

    override suspend fun setCurrentThemeOrdinal(themeOrdinal: Int): Outcome = Outcome.Success

    override suspend fun setCompactNavigation(compactNavigation: Boolean): Outcome =
        Outcome.Success

    override suspend fun setAutoHideBottomNavigation(autoHideBottomNavigation: Boolean): Outcome =
        Outcome.Success

    override suspend fun setAutoPlayTimelineVideos(autoPlayTimelineVideos: Boolean): Outcome =
        Outcome.Success

    override suspend fun setShowPostEngagementMetrics(showEngagementMetrics: Boolean): Outcome =
        Outcome.Success

    override suspend fun setShowTrendingTopics(showTrendingTopics: Boolean): Outcome =
        Outcome.Success

    override suspend fun setAllowAllTimelinePresentations(allowAllTimelinePresentations: Boolean): Outcome =
        Outcome.Success
}
