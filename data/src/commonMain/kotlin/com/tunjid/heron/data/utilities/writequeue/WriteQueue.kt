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
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

sealed class WriteQueue {

    internal abstract val postRepository: PostRepository
    internal abstract val profileRepository: ProfileRepository

    abstract suspend fun enqueue(
        writable: Writable,
    )

    abstract suspend fun drain()
}

class SnapshotWriteQueue @Inject constructor(
    override val postRepository: PostRepository,
    override val profileRepository: ProfileRepository,
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

