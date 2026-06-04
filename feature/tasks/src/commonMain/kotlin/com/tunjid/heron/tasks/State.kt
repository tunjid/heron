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
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tasks.ui.TaskItem
import com.tunjid.heron.ui.text.Memo
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        @Transient
        val inFlight: List<TaskItem.InFlight> = emptyList(),
        @Transient
        val failed: List<TaskItem.Failed> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(): Immutable = Immutable()
    }
}

sealed class Action(val key: String) {

    data class Retry(
        val failedWrite: FailedWrite,
    ) : Action(key = "Retry")

    data class Dismiss(
        val failedWrite: FailedWrite,
    ) : Action(key = "Dismiss")

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
