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

package com.tunjid.heron.di

import com.tunjid.heron.compose.di.ComposeComponent
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feed.di.FeedComponent
import com.tunjid.heron.gallery.di.GalleryComponent
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.messages.di.MessagesComponent
import com.tunjid.heron.notifications.di.NotificationsComponent
import com.tunjid.heron.postdetail.di.PostDetailComponent
import com.tunjid.heron.profile.avatar.di.ProfileAvatarComponent
import com.tunjid.heron.profile.di.ProfileComponent
import com.tunjid.heron.profiles.di.ProfilesComponent
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchComponent
import com.tunjid.heron.signin.di.SignInComponent
import com.tunjid.heron.splash.di.SplashComponent
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn


@DependencyGraph(
    scope = AppScope::class
)
interface AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes dataBindings: DataBindings,
            @Includes scaffoldBindings: ScaffoldBindings,
            @Includes signInComponent: SignInComponent,
            @Includes composeComponent: ComposeComponent,
            @Includes feedComponent: FeedComponent,
            @Includes galleryComponent: GalleryComponent,
            @Includes homeComponent: HomeComponent,
            @Includes messagesComponent: MessagesComponent,
            @Includes notificationsComponent: NotificationsComponent,
            @Includes postDetailComponent: PostDetailComponent,
            @Includes profileComponent: ProfileComponent,
            @Includes profileAvatarComponent: ProfileAvatarComponent,
            @Includes profilesComponent: ProfilesComponent,
            @Includes searchComponent: SearchComponent,
            @Includes splashComponent: SplashComponent,
        ): AppGraph
    }

    val entryMap: Map<String, PaneEntry<ThreePane, Route>>

    @SingleIn(AppScope::class)
    @Provides
    fun appState(
        notificationsRepository: NotificationsRepository,
        navigationStateHolder: NavigationStateHolder,
        videoPlayerController: VideoPlayerController,
        writeQueue: WriteQueue,
    ): AppState = AppState(
        entryMap = entryMap,
        notificationsRepository = notificationsRepository,
        navigationStateHolder = navigationStateHolder,
        videoPlayerController = videoPlayerController,
        writeQueue = writeQueue,
    )

    val appState: AppState
}
