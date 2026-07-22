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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo

// The backend is a LiteRT-LM concept; iOS inference runs on Apple Foundation Models, which manages
// its own execution. This actual only exists to satisfy the common [backendFor] expectation.
internal actual fun backendFor(): InferenceBackend =
    if (isSimulator()) InferenceBackend.Cpu else InferenceBackend.Gpu

// iOS runs inference through a Swift bridge, not the LiteRT-LM JNI, so there is no modified-UTF-8
// boundary to guard; prompts pass through unchanged.
internal actual fun sanitizeEnginePrompt(prompt: String): String = prompt

private fun isSimulator(): Boolean =
    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null

/**
 * Foundation Models needs iOS 26+. This is the coarse OS gate; actual device eligibility (Apple
 * Intelligence hardware + enablement) is resolved at runtime by the iOS inference manager, which
 * downgrades to [InferenceSource.None] when the model reports the device is ineligible.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun platformInferenceCapability(): InferenceSource =
    if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 26 })
        InferenceSource.Platform
    else
        InferenceSource.None
