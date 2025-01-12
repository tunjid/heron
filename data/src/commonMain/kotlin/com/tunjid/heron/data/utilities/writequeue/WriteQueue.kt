package com.tunjid.heron.data.utilities.writequeue

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

sealed class WriteQueue {

    internal abstract val postRepository: PostRepository

    abstract suspend fun enqueue(
        writable: Writable,
    )

    abstract suspend fun drain()
}

class SnapshotWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
) : WriteQueue() {
    // At some point this queue should be persisted to disk
    private val queue = mutableStateListOf<Writable>()

    override suspend fun enqueue(
        writable: Writable,
    ) {
        // Enqueue on main
        withContext(Dispatchers.Main) {
            // De-dup
            if (queue.any { writable.queueId == it.queueId }) return@withContext
            queue.add(writable)
        }
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
                    queue.removeLast()
                }
            }
    }
}

