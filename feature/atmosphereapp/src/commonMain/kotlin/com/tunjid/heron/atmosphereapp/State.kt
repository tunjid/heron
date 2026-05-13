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

package com.tunjid.heron.atmosphereapp

import androidx.compose.runtime.Stable
import com.tunjid.heron.atmosphereapp.AppScreenStateHolders.Records
import com.tunjid.heron.atmosphereapp.di.profileHandleOrId
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.models.RockSkyScrobble
import com.tunjid.heron.data.core.models.RockSkyTrack
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.avatarSharedElementKey
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.state.RecordStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val app: AtmosphereApp?,
        val profile: Profile?,
        val avatarSharedElementKey: String,
        val currentPage: Int = 0,
        @Transient
        val stateHolders: List<AppScreenStateHolders> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(
            route: Route,
        ): Immutable = Immutable(
            avatarSharedElementKey = route.avatarSharedElementKey ?: "",
            app = route.model<AtmosphereApp>(),
            profile = route.model<Profile>() ?: stubProfile(
                did = ProfileId(route.profileHandleOrId.id),
                handle = ProfileHandle(route.profileHandleOrId.id),
                avatar = null,
            ),
        )
    }
}

internal typealias AtmosphereAppStateHolder = ActionSuspendingStateMutator<Action, State>

@Stable
sealed class AppScreenStateHolders {

    @Stable
    sealed class Records<T : Record>(
        private val mutator: RecordStateHolder<T>,
    ) : AppScreenStateHolders(),
        RecordStateHolder<T> by mutator

    @Stable
    sealed class StandardSite<T : Record>(
        mutator: RecordStateHolder<T>,
    ) : Records<T>(mutator) {

        @Stable
        class Documents(
            mutator: RecordStateHolder<StandardDocument>,
        ) : StandardSite<StandardDocument>(mutator)

        @Stable
        class Publications(
            mutator: RecordStateHolder<StandardPublication>,
        ) : StandardSite<StandardPublication>(mutator)
    }

    @Stable
    sealed class Rocksky<T : Record>(
        mutator: RecordStateHolder<T>,
    ) : Records<T>(mutator) {

        @Stable
        class Albums(
            mutator: RecordStateHolder<RockSkyAlbum>,
        ) : Rocksky<RockSkyAlbum>(mutator)

        @Stable
        class Tracks(
            mutator: RecordStateHolder<RockSkyTrack>,
        ) : Rocksky<RockSkyTrack>(mutator)

        @Stable
        class Artists(
            mutator: RecordStateHolder<RockSkyArtist>,
        ) : Rocksky<RockSkyArtist>(mutator)

        @Stable
        class Scrobbles(
            mutator: RecordStateHolder<RockSkyScrobble>,
        ) : Rocksky<RockSkyScrobble>(mutator)
    }

    val key: String
        get() = when (this) {
            is StandardSite.Documents -> StandardDocumentUri.NAMESPACE
            is StandardSite.Publications -> StandardPublicationUri.NAMESPACE
            is Rocksky.Albums -> AlbumUri.NAMESPACE
            is Rocksky.Tracks -> TrackUri.NAMESPACE
            is Rocksky.Artists -> ArtistUri.NAMESPACE
            is Rocksky.Scrobbles -> ScrobbleUri.NAMESPACE
        }

    fun refresh() = (this as Records<*>).accept(TilingState.Action.Refresh)
}

val AppScreenStateHolders?.isRefreshing: Boolean
    get() = when (this) {
        is Records<*> -> state.isRefreshing
        null -> false
    }

sealed class Action(val key: String) {

    data class PageChanged(
        val page: Int,
    ) : Action(key = "PageChanged")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class TogglePublicationSubscription : Action(key = "TogglePublicationSubscription") {
        data class Subscribe(
            val publicationUri: StandardPublicationUri,
        ) : TogglePublicationSubscription()

        data class Unsubscribe(
            val subscriptionUri: StandardSubscriptionUri,
        ) : TogglePublicationSubscription()
    }

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
