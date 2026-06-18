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

package com.tunjid.heron.tasks

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.tasks.ui.failedTaskItem
import com.tunjid.heron.tasks.ui.inFlightTaskItem
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.tasks.generated.resources.Res
import heron.feature.tasks.generated.resources.dismiss_failed
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

internal typealias TasksStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualTasksViewModel
}

@Stable
@AssistedInject
class ActualTasksViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    TasksStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadInFlightWrites(
                state = state,
                authRepository = authRepository,
                recordRepository = recordRepository,
                writeQueue = writeQueue,
            )
            launchLoadFailedWrites(
                state = state,
                authRepository = authRepository,
                recordRepository = recordRepository,
                writeQueue = writeQueue,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Retry -> action.flow.launchRetryMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.Dismiss -> action.flow.launchDismissMutations(
                        writeQueue = writeQueue,
                        state = state,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )
                    is Action.Navigate -> action.flow.launchedCollect {
                        navActions(it.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadInFlightWrites(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    writeQueue: WriteQueue,
) {
    combine(
        flow = recordRepository.associatedRecords(
            writeQueue.queueChanges,
            Writable::embeddableRecordUri,
        ),
        flow2 = authRepository.signedInUser,
    ) { (inFlightWrites, embeddedRecords), signedInProfile ->
        inFlightWrites.map { writable ->
            writable.inFlightTaskItem(
                associatedRecord = writable.associatedRecord(
                    records = embeddedRecords,
                    signedInProfile = signedInProfile,
                ),
            )
        }
    }
        .launchedCollect(state::inFlight::set)
}

context(productionScope: CoroutineScope)
private fun launchLoadFailedWrites(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
    writeQueue: WriteQueue,
) {
    combine(
        flow = recordRepository.associatedRecords(
            writeQueue.failedWrites,
        ) { it.writable.embeddableRecordUri() },
        flow2 = authRepository.signedInUser,
    ) { (failedWrites, embeddedRecords), signedInProfile ->
        failedWrites.map { failedWrite ->
            failedWrite.failedTaskItem(
                associatedRecord = failedWrite.writable.associatedRecord(
                    records = embeddedRecords,
                    signedInProfile = signedInProfile,
                ),
            )
        }
    }
        .launchedCollect(state::failed::set)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Retry>.launchRetryMutations(
    writeQueue: WriteQueue,
) = launchedCollectLatest { action ->
    writeQueue.retry(action.failedWrite)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Dismiss>.launchDismissMutations(
    writeQueue: WriteQueue,
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    if (writeQueue.dismiss(action.failedWrite) is Outcome.Failure) {
        state.messages += Memo.Resource(Res.string.dismiss_failed)
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun <T> RecordRepository.associatedRecords(
    items: Flow<List<T>>,
    mapper: (T) -> EmbeddableRecordUri?,
): Flow<Pair<List<T>, Map<EmbeddableRecordUri, Record.Embeddable>>> {
    val sharedItems = items.shareIn(
        scope = productionScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1,
    )
    return combine(
        flow = sharedItems,
        flow2 = sharedItems.map { items ->
            items.mapNotNullTo(
                destination = mutableSetOf(),
                transform = mapper,
            )
        }
            .distinctUntilChanged()
            .flatMapLatest(::embeddableRecords),
        transform = { first, second -> Pair(first, second.associateBy { it.embeddableRecordUri }) },
    )
}

private fun Writable.associatedRecord(
    records: Map<EmbeddableRecordUri, Record.Embeddable>,
    signedInProfile: Profile?,
): Record.Embeddable? = when (this) {
    is Writable.Create -> signedInProfile?.let(request::stubPost)
    else -> embeddableRecordUri()?.let(records::get)
}

/**
 * Synthesizes an optimistic [Post] from a [Post.Create.Request] so the in-flight task card can
 * preview the post being created. Only the fields read by the quoted-post preview (author, text,
 * timestamp, media embed) are populated; identifiers are placeholders and the record is read only.
 */
private fun Post.Create.Request.stubPost(
    author: Profile,
): Post {
    val now = Clock.System.now()
    val photos = metadata.embeddedMedia.filterIsInstance<File.Media.Photo>()
    val video = metadata.embeddedMedia.filterIsInstance<File.Media.Video>().firstOrNull()
    return Post(
        cid = PostId(Uuid.random().toString()),
        uri = PostUri("${author.did.id}/${PostUri.NAMESPACE}/${RecordKey.generate().value}"),
        author = author,
        replyCount = 0,
        repostCount = 0,
        likeCount = 0,
        quoteCount = 0,
        indexedAt = now,
        embed = when {
            photos.isNotEmpty() -> ImageList(
                images = photos.map { photo ->
                    Image(
                        thumb = ImageUri(photo.uri.uri),
                        fullsize = ImageUri(photo.uri.uri),
                        alt = photo.altText.orEmpty(),
                        width = photo.width.toLong(),
                        height = photo.height.toLong(),
                    )
                },
            )
            video != null -> Video(
                cid = GenericId(Uuid.random().toString()),
                playlist = GenericUri(video.uri.uri),
                thumbnail = ImageUri(video.uri.uri),
                alt = video.altText,
                width = video.width.toLong(),
                height = video.height.toLong(),
            )
            else -> null
        },
        record = Post.Record(
            text = text,
            createdAt = now,
            links = links,
        ),
        viewerStats = null,
        labels = emptyList(),
    )
}

private fun Writable.embeddableRecordUri(): EmbeddableRecordUri? =
    when (this) {
        is Writable.Connection -> null
        is Writable.Create -> null
        is Writable.FeedList.AddMember -> create.listUri
        is Writable.Interaction -> this.interaction.postUri
        is Writable.NotificationUpdate -> null
        is Writable.ProfileUpdate -> null
        is Writable.Reaction -> null
        is Writable.RecordDeletion -> this.recordUri as? EmbeddableRecordUri
        is Writable.Restriction -> null
        is Writable.Send -> null
        is Writable.StandardSite.Subscribe -> this.create.publicationUri
        is Writable.StandardSite.UpdatePostReference -> this.reference.documentUri
        is Writable.StatusUpdate -> null
        is Writable.TimelineUpdate -> null
    }
