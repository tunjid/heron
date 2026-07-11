package com.tunjid.heron.sheets.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.timeout

@Stable
internal interface ProfileSearchStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<ProfileSearchAction, ProfileSearchState>

@AssistedFactory
internal fun interface ProfileSearchViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): ProfileSearchViewModel
}

internal class ProfileSearchViewModel(
    mutator: ActionSuspendingStateMutator<ProfileSearchAction, ProfileSearchState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ProfileSearchStateHolder,
    ActionSuspendingStateMutator<ProfileSearchAction, ProfileSearchState> by mutator {
    @AssistedInject
    constructor(
        searchRepository: SearchRepository,
        profileRepository: ProfileRepository,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = ProfileSearchState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = ProfileSearchAction::key,
                ) {
                    when (val action = type()) {
                        is ProfileSearchAction.UpdateTitle -> action.flow.launchUpdateTitleMutations(
                            state = state,
                        )
                        is ProfileSearchAction.Query -> action.flow.launchSearchMutations(
                            state = state,
                            searchRepository = searchRepository,
                        )
                        is ProfileSearchAction.Seed -> action.flow.launchSeedMutations(
                            state = state,
                            profileRepository = profileRepository,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun Flow<ProfileSearchAction.UpdateTitle>.launchUpdateTitleMutations(
    state: ProfileSearchState.SnapshotMutable,
) = launchedCollectLatest { action ->
    state.title = action.title
}

context(productionScope: CoroutineScope)
private fun Flow<ProfileSearchAction.Query>.launchSearchMutations(
    state: ProfileSearchState.SnapshotMutable,
    searchRepository: SearchRepository,
) {
    val shared = shareIn(
        scope = productionScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1,
    )
    shared.launchedCollect {
        when (it) {
            ProfileSearchAction.Query.Clear -> {
                state.text = ""
                state.searchResults = emptyList()
            }
            is ProfileSearchAction.Query.Search -> state.text = it.query
        }
    }
    shared.filterIsInstance<ProfileSearchAction.Query.Search>()
        .debounce(SEARCH_DEBOUNCE)
        .launchedCollectLatest { action ->
            searchRepository.autoCompleteProfileSearch(
                query = SearchQuery.OfProfiles(
                    query = action.query,
                    isLocalOnly = false,
                    data = CursorQuery.Data(
                        page = 0,
                        cursorAnchor = Clock.System.now(),
                        limit = MAX_SUGGESTED_PROFILES.toLong(),
                    ),
                ),
                cursor = Cursor.Initial,
            ).collect { profiles ->
                state.searchResults = profiles.map(ProfileWithViewerState::profile)
            }
        }
}

context(productionScope: CoroutineScope)
private fun Flow<ProfileSearchAction.Seed>.launchSeedMutations(
    state: ProfileSearchState.SnapshotMutable,
    profileRepository: ProfileRepository,
) = launchedCollectLatest { action ->
    state.cachedProfiles = (
        state.cachedProfiles + action.ids
            .chunked(SEED_BATCH_SIZE)
            .flatMap { batch ->
                coroutineScope {
                    batch.map { profileId ->
                        async {
                            state.cachedProfiles
                                .firstOrNull { it.did == profileId || it.handle == profileId }
                                ?: profileRepository.profile(profileId)
                                    .map<Profile, Profile?> { it }
                                    .timeout(5.seconds)
                                    .catch { exception ->
                                        if (exception is TimeoutCancellationException) emit(null)
                                        else throw exception
                                    }
                                    .firstOrNull()
                        }
                    }.awaitAll()
                }
            }
            .filterNotNull()
        )
        .distinctBy(Profile::did)
}

@Snapshottable
internal interface ProfileSearchState {
    @SnapshotSpec
    data class Immutable(
        val title: String = "",
        val text: String = "",
        val searchResults: List<Profile> = emptyList(),
        val cachedProfiles: List<Profile> = emptyList(),
    ) : ProfileSearchState
}

internal sealed class ProfileSearchAction(
    val key: String,
) {
    sealed class Query : ProfileSearchAction("Query") {
        data class Search(
            val query: String,
        ) : Query()

        data object Clear : Query()
    }

    data class UpdateTitle(
        val title: String,
    ) : ProfileSearchAction("UpdateTitle")

    data class Seed(
        val ids: List<Id.Profile>,
    ) : ProfileSearchAction("Seed")
}

private val SEARCH_DEBOUNCE = 3.milliseconds

const val MAX_SUGGESTED_PROFILES = 5
const val SEED_BATCH_SIZE = 5
