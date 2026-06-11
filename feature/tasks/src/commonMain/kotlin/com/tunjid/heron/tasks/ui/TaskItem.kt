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

package com.tunjid.heron.tasks.ui

import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.timeline.utilities.WritableDescription
import com.tunjid.heron.timeline.utilities.describe

sealed class TaskItem {
    abstract val writable: Writable
    abstract val description: WritableDescription
    abstract val associatedRecord: Record.Embeddable?

    data class InFlight(
        override val writable: Writable,
        override val description: WritableDescription,
        override val associatedRecord: Record.Embeddable?,
    ) : TaskItem()

    data class Failed(
        val failedWrite: FailedWrite,
        override val description: WritableDescription,
        override val associatedRecord: Record.Embeddable?,
    ) : TaskItem() {
        override val writable: Writable get() = failedWrite.writable
    }
}

internal fun Writable.inFlightTaskItem(
    associatedRecord: Record.Embeddable?,
): TaskItem.InFlight = TaskItem.InFlight(
    writable = this,
    description = describe(),
    associatedRecord = associatedRecord,
)

internal fun FailedWrite.failedTaskItem(
    associatedRecord: Record.Embeddable?,
): TaskItem.Failed = TaskItem.Failed(
    failedWrite = this,
    description = writable.describe(),
    associatedRecord = associatedRecord,
)
