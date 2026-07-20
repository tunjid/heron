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

package com.tunjid.heron.data.tasks

import com.tunjid.heron.data.core.utilities.File
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** A stable, platform-unique identifier for a [Task]. */
@Serializable
@JvmInline
value class TaskId(
    val value: String,
) {
    override fun toString(): String = value
}

/**
 * A unit of long-running background work handed off to the platform for execution.
 *
 * A [Task] is deliberately declarative: the platform runs it out of band — an Android
 * user-initiated data-transfer job, an iOS background `URLSession`, or a desktop coroutine —
 * and reports a [TaskStatus]. Nothing in this module pumps the work itself.
 */
@Serializable
sealed interface Task {
    /** Stable identity; doubles as the platform-unique key used to deduplicate work. */
    val id: TaskId

    val kind: Kind

    enum class Kind {
        Transfer,
    }

    /**
     * A file download. [destination] is the on-device file to write the download to. [auth] is
     * a reference to how the request should be authenticated; the credential itself is resolved at
     * run time and is never persisted with the task.
     */
    @Serializable
    data class Download(
        val sourceUrl: String,
        val destination: File.System,
        val sizeInBytes: Long,
        val sha256: String? = null,
    ) : Task {
        override val id: TaskId
            get() = TaskId("download:${destination.relativePath}")

        override val kind: Kind
            get() = Kind.Transfer
    }
}
