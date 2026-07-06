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

package com.tunjid.heron.postdetail

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.appliedLabels
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.model
import com.tunjid.heron.ui.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.ui.text.Memo
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
        val anchorPost: Post?,
        val sharedElementPrefix: String,
        @Transient
        val currentLanguageTag: String? = null,
        @Transient
        val postLanguageTag: String? = null,
        @Transient
        val order: TimelineItem.Threaded.Order? = null,
        @Transient
        val viewMode: TimelineItem.Threaded.ViewMode = TimelineItem.Threaded.ViewMode.Linear,
        @Transient
        val source: Timeline.Source? = null,
        @Transient
        val timelinePosition: CursorQuery.Data? = null,
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val signedInProfileId: ProfileId? = null,
        @Transient
        val items: List<TimelineItem> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(route: Route): Immutable {
            val anchorPost = route.model<Post>()
            return Immutable(
                anchorPost = anchorPost,
                sharedElementPrefix = route.sharedElementPrefix,
                source = route.model(),
                timelinePosition = route.model(),
                items = when (anchorPost) {
                    null -> TimelineItem.LoadingItems
                    else -> listOf(
                        TimelineItem.Threaded.Linear(
                            id = anchorPost.uri.uri,
                            anchorPostIndex = 0,
                            nodes = listOf(
                                TimelineItem.Threaded.Node(
                                    post = anchorPost,
                                    threadGate = null,
                                    appliedLabels = route.model<AppliedLabels>()
                                        ?: anchorPost.appliedLabels(
                                            adultContentEnabled = false,
                                            labelers = emptyList(),
                                            labelPreferences = emptyList(),
                                        ),
                                    isMuted = false,
                                ),
                            ),
                            generation = 0,
                            hasBreak = false,
                            signedInProfileId = null,
                        ),
                    )
                },
            )
        }
    }
}

val State.canTranslate: Boolean get() {
    val currentLanguageTag = currentLanguageTag ?: return false
    val postLanguageTag = postLanguageTag ?: return false

    return !currentLanguageTag.startsWith(postLanguageTag) &&
        !postLanguageTag.startsWith(currentLanguageTag)
}

sealed class Action(val key: String) {

    sealed class Load : Action(key = "Load") {
        data object Initial : Load()
        data class Order(
            val order: TimelineItem.Threaded.Order,
        ) : Load()
        data class ViewMode(
            val viewMode: TimelineItem.Threaded.ViewMode,
        ) : Load()
    }

    data class BlockAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "BlockAccount")

    data class MuteAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "MuteAccount")

    data class DeleteRecord(
        val recordUri: RecordUri,
    ) : Action(key = "DeleteRecord")

    data class TogglePublicationSubscription(
        val publication: StandardPublication,
    ) : Action(key = "TogglePublicationSubscription")

    data class UpdateCurrentLanguageTag(
        val languageTag: String,
    ) : Action(key = "UpdateCurrentLanguageTag")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

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
