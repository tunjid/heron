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
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.network.NetworkConnectionException
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.onEachSignedInProfile
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
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
                    pendingWrites.any { writable.queueId == it.queueId } -> {
                        status = Status.Duplicate
                        this
                    }
                    else -> {
                        status = Status.Enqueued
                        copy(pendingWrites = listOf(writable) + pendingWrites)
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
                for (writable in writes.asReversed()) {
                    val shouldEmit = concurrentWriteMutex.withLock {
                        processingWriteIds.add(writable.queueId)
                    }
                    if (shouldEmit) emit(writable)
                }
            }
            .buffer()
            .flatMapMerge(concurrency = MaxConcurrentWrites) { writable ->
                concurrentWrite(writable)
            }
            .collect { (writable, outcome) ->
                concurrentWriteMutex.withLock {
                    savedStateDataSource.updateWrites {
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
                            pendingWrites = when {
                                shouldTryAgain -> pendingWrites
                                else ->
                                    pendingWrites
                                        .filter { it.queueId != writable.queueId }
                                        .take(MaximumPendingWrites)
                            },
                        )
                    }
                    processingWriteIds.remove(writable.queueId)
                }
            }
    }

    private fun PersistedWriteQueue.concurrentWrite(
        writable: Writable,
    ) = flow {
        emit(
            Pair(
                writable,
                with(writable) { withTimeout(WriteTimeout) { write() } },
            ),
        )
    }
        .catch {
            emit(writable to Outcome.Failure(it))
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

private val WriteTimeout = 10.seconds
private const val MaxConcurrentWrites = 6
private const val MaximumPendingWrites = 15
private const val MaximumFailedWrites = 10
