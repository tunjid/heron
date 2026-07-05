/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.inference

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
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
        val models: List<ModelItem> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(
            models: List<InferenceModel>,
        ): Immutable = Immutable(
            models = models.map { model -> ModelItem(model = model) },
        )
    }
}

/** A single available [model] paired with its current download [status]. */
@Stable
data class ModelItem(
    val model: InferenceModel,
    val status: TaskStatus = TaskStatus.NotFound,
)

sealed class Action(val key: String) {

    data class Download(
        val model: InferenceModel,
    ) : Action(key = "Download")

    data class Cancel(
        val model: InferenceModel,
    ) : Action(key = "Cancel")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}
