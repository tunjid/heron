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

import android.os.Build

/**
 * GPU by default, falling back to CPU on the Pixel 10 (whose GPU delegate is unreliable for
 * LiteRT-LM, mirroring Google's AI Edge Gallery) and on emulators (no usable GPU delegate).
 */
internal actual fun backendFor(): InferenceBackend =
    if (isPixel10() || isEmulator()) InferenceBackend.Cpu
    else InferenceBackend.Gpu

// The Android LiteRT-LM binding handles supplementary characters correctly,
// so prompts pass through unchanged.
internal actual fun sanitizeEnginePrompt(prompt: String): String = prompt

// Android runs the downloadable LiteRT-LM catalog.
actual fun platformInferenceCapability(): InferenceSource = InferenceSource.External

private fun isPixel10(): Boolean =
    Build.MODEL.contains("pixel 10", ignoreCase = true)

// Standard Build-property heuristics covering Android Studio AVDs (goldfish/ranchu, sdk_gphone*)
// and Genymotion.
private fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.FINGERPRINT.contains("emulator") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for") ||
        Build.MODEL.contains("sdk_gphone") ||
        Build.PRODUCT.contains("sdk") ||
        Build.PRODUCT.contains("emulator") ||
        Build.PRODUCT.contains("simulator") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
