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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.tasks.Progress
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.inference.di.Route as InferenceRoute
import com.tunjid.heron.inference.ui.ModelCard
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.heron.ui.scaffold.ui.theme.AppTheme
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf

@Preview
@Composable
internal fun InferencePreview() {
    val scope = rememberCoroutineScope()
    RoutePreview(
        route = routeOf(path = "/inference"),
        routeStateHolder = remember(scope) {
            ActualInferenceViewModel(
                mutator = State.Immutable(
                    models = listOf(
                        ModelItem(
                            model = InferenceModel.Gemma31B,
                            status = ModelStatus.Pending(
                                TaskStatus.Running(
                                    progress = Progress(
                                        completedBytes = 210_000_000L,
                                        totalBytes = InferenceModel.Gemma31B.sizeInBytes,
                                    ),
                                ),
                            ),
                        ),
                        ModelItem(
                            model = InferenceModel.Gemma4E2B,
                            status = ModelStatus.Pending(
                                TaskStatus.NotFound,
                            ),
                        ),
                        ModelItem(
                            model = InferenceModel.Gemma4E4B,
                            status = ModelStatus.Downloaded(
                                LoadedModel(
                                    model = InferenceModel.Gemma4E4B,
                                    file = File.System(
                                        relativePath = InferenceModel.Gemma4E4B.fileName,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ).asNoOpActionSuspendingStateMutator(),
                scope = scope,
            )
        },
        render = { route, paneScaffoldState ->
            InferenceRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}

@Preview
@Composable
internal fun ModelCardMemoryWarningPreview() {
    AppTheme {
        Surface {
            ModelCard(
                modifier = Modifier.padding(16.dp),
                engineState = null,
                item = ModelItem(
                    model = InferenceModel.Gemma4E4B,
                ),
                // A 4 GB reading sits below the E4B model's 12 GB minimum, so the low-memory
                // warning renders regardless of how much RAM the machine running the preview has.
                deviceMemoryBytes = 4L * 1024 * 1024 * 1024,
                onLoad = {},
                onDownload = {},
                onCancel = {},
                onDelete = {},
            )
        }
    }
}
