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

package com.tunjid.heron.data.ml.engine

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Builds the iOS engine from the Swift-provided [bridge]. Call from the app entry
 * point (`createAppState`) with the bridge Swift hands in, then inject the result —
 * no global state. Exposed to Swift as `InferenceEngine_iosKt.createInferenceEngine`.
 */
fun createInferenceEngine(
    bridge: IosInferenceBridge,
    ioDispatcher: CoroutineDispatcher,
): InferenceEngine = IosInferenceEngine(bridge, ioDispatcher)
