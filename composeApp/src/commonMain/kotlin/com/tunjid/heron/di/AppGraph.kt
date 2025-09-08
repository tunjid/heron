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

import com.tunjid.heron.compose.di.ComposeBindings
import com.tunjid.heron.conversation.di.ConversationBindings
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feed.di.FeedBindings
import com.tunjid.heron.gallery.di.GalleryBindings
import com.tunjid.heron.home.di.HomeBindings
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.list.di.ListBindings
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.messages.di.MessagesBindings
import com.tunjid.heron.notifications.di.NotificationsBindings
import com.tunjid.heron.postdetail.di.PostDetailBindings
import com.tunjid.heron.posts.di.PostsBindings
import com.tunjid.heron.profile.avatar.di.ProfileAvatarBindings
import com.tunjid.heron.profile.di.ProfileBindings
import com.tunjid.heron.profiles.di.ProfilesBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchBindings
import com.tunjid.heron.settings.di.SettingsBindings
import com.tunjid.heron.signin.di.SignInBindings
import com.tunjid.heron.splash.di.SplashBindings
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(
    scope = AppScope::class,
)
interface AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Includes dataBindings: DataBindings,
            @Includes scaffoldBindings: ScaffoldBindings,
            @Includes signInBindings: SignInBindings,
            @Includes composeBindings: ComposeBindings,
            @Includes conversationBindings: ConversationBindings,
            @Includes feedBindings: FeedBindings,
            @Includes galleryBindings: GalleryBindings,
            @Includes homeBindings: HomeBindings,
            @Includes listBindings: ListBindings,
            @Includes messagesBindings: MessagesBindings,
            @Includes notificationsBindings: NotificationsBindings,
            @Includes postDetailBindings: PostDetailBindings,
            @Includes postsBindings: PostsBindings,
            @Includes profileBindings: ProfileBindings,
            @Includes profileAvatarBindings: ProfileAvatarBindings,
            @Includes profilesBindings: ProfilesBindings,
            @Includes searchBindings: SearchBindings,
            @Includes splashBindings: SplashBindings,
            @Includes settingsBindings: SettingsBindings,
        ): AppGraph
    }

    val entryMap: Map<String, PaneEntry<ThreePane, Route>>

    @SingleIn(AppScope::class)
    @Provides
    fun appState(
        authRepository: AuthRepository,
        notificationsRepository: NotificationsRepository,
        navigationStateHolder: NavigationStateHolder,
        imageLoader: ImageLoader,
        videoPlayerController: VideoPlayerController,
        writeQueue: WriteQueue,
    ): AppState = AppState(
        entryMap = entryMap,
        authRepository = authRepository,
        notificationsRepository = notificationsRepository,
        navigationStateHolder = navigationStateHolder,
        imageLoader = imageLoader,
        videoPlayerController = videoPlayerController,
        writeQueue = writeQueue,
    )

    val appState: AppState
}
