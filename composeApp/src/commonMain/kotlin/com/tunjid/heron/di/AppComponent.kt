/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.di

import com.tunjid.heron.compose.di.ComposeComponent
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feed.di.FeedComponent
import com.tunjid.heron.gallery.di.GalleryComponent
import com.tunjid.heron.home.di.HomeComponent
import com.tunjid.heron.messages.di.MessagesComponent
import com.tunjid.heron.notifications.di.NotificationsComponent
import com.tunjid.heron.postdetail.di.PostDetailComponent
import com.tunjid.heron.profile.di.ProfileComponent
import com.tunjid.heron.profiles.di.ProfilesComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.heron.search.di.SearchComponent
import com.tunjid.heron.signin.di.SignInComponent
import com.tunjid.heron.splash.di.SplashComponent
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

@KmpComponentCreate
expect fun AppComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
    signInComponent: SignInComponent,
    composeComponent: ComposeComponent,
    feedComponent: FeedComponent,
    galleryComponent: GalleryComponent,
    homeComponent: HomeComponent,
    messagesComponent: MessagesComponent,
    notificationsComponent: NotificationsComponent,
    postDetailComponent: PostDetailComponent,
    profileComponent: ProfileComponent,
    profilesComponent: ProfilesComponent,
    searchComponent: SearchComponent,
    splashComponent: SplashComponent,
): AppComponent

@Component
abstract class AppComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
    @Component val signInComponent: SignInComponent,
    @Component val composeComponent: ComposeComponent,
    @Component val feedComponent: FeedComponent,
    @Component val galleryComponent: GalleryComponent,
    @Component val homeComponent: HomeComponent,
    @Component val messagesComponent: MessagesComponent,
    @Component val notificationsComponent: NotificationsComponent,
    @Component val postDetailComponent: PostDetailComponent,
    @Component val profileComponent: ProfileComponent,
    @Component val profilesComponent: ProfilesComponent,
    @Component val searchComponent: SearchComponent,
    @Component val splashComponent: SplashComponent,
) {

    abstract val routeConfigurationMap: Map<String, PaneStrategy<ThreePane, Route>>

    @Provides
    fun appState(
        navigationStateHolder: NavigationStateHolder,
        savedStateRepository: SavedStateRepository,
        writeQueue: WriteQueue,
    ): AppState = AppState(
        routeConfigurationMap = routeConfigurationMap,
        navigationStateHolder = navigationStateHolder,
        writeQueue = writeQueue,
    )

    abstract val appState: AppState

    companion object
}
