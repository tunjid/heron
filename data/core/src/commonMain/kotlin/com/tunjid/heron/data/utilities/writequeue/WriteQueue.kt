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

package com.tunjid.heron.data.utilities.writequeue

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.network.NetworkConnectionException
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.onEachSignedInProfile
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import dev.zacsweers.metro.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.IOException

sealed class WriteQueue {

    internal abstract val postRepository: PostRepository
    internal abstract val profileRepository: ProfileRepository

    internal abstract val messageRepository: MessageRepository

    internal abstract val timelineRepository: TimelineRepository
    internal abstract val notificationRepository: NotificationsRepository

    internal abstract val recordRepository: RecordRepository

    abstract val queueChanges: Flow<List<Writable>>

    abstract suspend fun enqueue(
        writable: Writable,
    ): Status

    abstract suspend fun awaitDequeue(
        writable: Writable,
    )

    abstract suspend fun drain()

    sealed interface Status {
        data object Enqueued : Status
        data object Dropped : Status
        data object Duplicate : Status
    }
}

internal class SnapshotWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
    override val profileRepository: ProfileRepository,
    override val messageRepository: MessageRepository,
    override val timelineRepository: TimelineRepository,
    override val notificationRepository: NotificationsRepository,
    override val recordRepository: RecordRepository,
) : WriteQueue() {
    // At some point this queue should be persisted to disk
    private val queue = mutableStateListOf<Writable>()

    override val queueChanges: Flow<List<Writable>>
        get() = snapshotFlow { queue.toList() }
            .distinctUntilChangedBy {
                it.map(Writable::queueId)
            }

    override suspend fun enqueue(
        writable: Writable,
    ): Status {
        // Enqueue on main
        return withContext(Dispatchers.Main) {
            // De-dup
            if (queue.any { writable.queueId == it.queueId }) return@withContext Status.Duplicate
            queue.add(writable)
            Status.Enqueued
        }
    }

    override suspend fun awaitDequeue(writable: Writable) {
        snapshotFlow {
            queue.firstOrNull { it.queueId == writable.queueId }
        }.first { it == null }
    }

    override suspend fun drain() {
        snapshotFlow { queue.lastOrNull() }
            .filterNotNull()
            .collect { writable ->
                withContext(Dispatchers.IO) {
                    with(writable) {
                        write()
                    }
                }
                // Dequeue on main
                withContext(Dispatchers.Main) {
                    queue.removeAt(queue.lastIndex)
                }
            }
    }
}

internal class PersistedWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
    override val profileRepository: ProfileRepository,
    override val messageRepository: MessageRepository,
    override val timelineRepository: TimelineRepository,
    private val savedStateDataSource: SavedStateDataSource,
    override val notificationRepository: NotificationsRepository,
    override val recordRepository: RecordRepository,
) : WriteQueue() {

    private val processingWriteIds = mutableSetOf<String>()
    private val concurrentWriteMutex = Mutex()

    override val queueChanges: Flow<List<Writable>>
        get() = savedStateDataSource.signedInProfileWrites()

    override suspend fun enqueue(
        writable: Writable,
    ): Status {
        concurrentWriteMutex.withLock {
            if (writable.queueId in processingWriteIds) return Status.Duplicate
        }
        var status: Status = Status.Dropped
        savedStateDataSource.inCurrentProfileSession {
            savedStateDataSource.updateWrites {
                when {
                    pendingWrites.size >= MaximumPendingWrites -> {
                        status = Status.Dropped
                        this
                    }
                    pendingWrites.any { writable.queueId == it.queueId } -> {
                        status = Status.Duplicate
                        this
                    }
                    else -> {
                        status = Status.Enqueued
                        copy(pendingWrites = pendingWrites + writable)
                    }
                }
            }
        }
        return status
    }

    override suspend fun awaitDequeue(writable: Writable) {
        savedStateDataSource.signedInProfileWrites()
            .first { writes ->
                writes.none { it.queueId == writable.queueId }
            }
    }

    override suspend fun drain() = savedStateDataSource.onEachSignedInProfile {
        savedStateDataSource.signedInProfileWrites()
            .transform { writes ->
                filterAndRecordNewWrites(writes)
            }
            .buffer()
            .flatMapMerge(concurrency = MaxConcurrentWrites) { writable ->
                concurrentWrite(writable)
            }
            .collect { (writable, outcome) ->
                onWriteOutcome(outcome, writable)
            }
    }

    private suspend fun FlowCollector<Writable>.filterAndRecordNewWrites(
        writes: List<Writable>,
    ) {
        for (writable in writes) {
            var inserted = false
            try {
                inserted = maybeInsertIntoConcurrentProcessingQueue(writable)
                if (inserted) emit(writable)
            } catch (e: CancellationException) {
                if (inserted) removeFromConcurrentProcessingQueue(writable)
                throw e
            }
        }
    }

    private fun concurrentWrite(
        writable: Writable,
    ) = flow {
        emit(
            Pair(
                first = writable,
                second = with(writable) {
                    withTimeout(writable.writeTimeout()) { write() }
                },
            ),
        )
    }
        .catch {
            emit(writable to Outcome.Failure(it))
        }

    private suspend fun onWriteOutcome(
        outcome: Outcome,
        writable: Writable,
    ) {
        val failure = when (outcome) {
            is Outcome.Failure -> outcome.exception
            else -> null
        }
        val shouldTryAgain = when (failure) {
            is NetworkConnectionException,
            is TimeoutCancellationException,
            -> true
            else -> false
        }

        if (shouldTryAgain) {
            removeFromConcurrentProcessingQueue(writable)
        }

        try {
            savedStateDataSource.updateWrites {
                copy(
                    failedWrites = when {
                        failure != null && !shouldTryAgain ->
                            failedWrites
                                .plus(
                                    FailedWrite(
                                        writable = writable,
                                        failedAt = Clock.System.now(),
                                        reason = when (failure) {
                                            is IOException -> FailedWrite.Reason.IO
                                            else -> null
                                        },
                                    ),
                                )
                                .distinctBy { it.writable.queueId }
                                .takeLast(MaximumFailedWrites)
                        else -> failedWrites
                    },
                    pendingWrites = pendingWrites
                        .filter { it.queueId != writable.queueId }
                        .let { writes -> if (shouldTryAgain) writes + writable else writes },
                )
            }
        } finally {
            if (!shouldTryAgain) {
                removeFromConcurrentProcessingQueue(writable)
            }
        }
    }

    private suspend fun maybeInsertIntoConcurrentProcessingQueue(
        writable: Writable,
    ) = concurrentWriteMutex.withLock {
        processingWriteIds.add(writable.queueId)
    }

    private suspend fun removeFromConcurrentProcessingQueue(
        writable: Writable,
    ) = withContext(NonCancellable) {
        concurrentWriteMutex.withLock {
            processingWriteIds.remove(writable.queueId)
        }
    }
}

private fun SavedStateDataSource.signedInProfileWrites() =
    singleAuthorizedSessionFlow { signedInProfileId ->
        savedState
            .mapNotNull { savedState ->
                savedState
                    // This should always be true, being doubly sure doesn't hurt however
                    .takeIf { it.auth?.authProfileId == signedInProfileId }
                    ?.signedInProfileData
                    ?.writes
                    ?.pendingWrites
            }
            .distinctUntilChangedBy { pendingWrites ->
                pendingWrites.map(Writable::queueId)
            }
    }

private suspend inline fun SavedStateDataSource.updateWrites(
    crossinline block: SavedState.Writes.(signedInProfileId: ProfileId?) -> SavedState.Writes,
) {
    updateSignedInProfileData { signedInProfileId ->
        copy(writes = writes.block(signedInProfileId))
    }
}

private fun Writable.writeTimeout() =
    when (this) {
        is Writable.Create ->
            request.metadata
                .embeddedMedia
                .fold(BasicWriteTimeout) { timeout, media ->
                    timeout + when (media) {
                        is File.Media.Photo -> ImageWriteTimeout
                        is File.Media.Video -> VideoWriteTimeout
                    }
                }
        is Writable.ProfileUpdate ->
            BasicWriteTimeout
                .plus(if (update.avatarFile != null) ImageWriteTimeout else 0.seconds)
                .plus(if (update.bannerFile != null) ImageWriteTimeout else 0.seconds)
        is Writable.Connection,
        is Writable.Interaction,
        is Writable.NotificationUpdate,
        is Writable.Reaction,
        is Writable.Restriction,
        is Writable.Send,
        is Writable.TimelineUpdate,
        is Writable.RecordDeletion,
        is Writable.FeedList,
        -> BasicWriteTimeout
    }

private val VideoWriteTimeout = 8.minutes

private val ImageWriteTimeout = 20.seconds
private val BasicWriteTimeout = 10.seconds
private const val MaxConcurrentWrites = 9
private const val MaximumPendingWrites = 15
private const val MaximumFailedWrites = 10
