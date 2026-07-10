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

package com.tunjid.heron.inference.ui

import androidx.compose.runtime.Composable

/**
 * The device's total physical memory in bytes, or `0` if it can't be determined.
 *
 * Used to warn before loading an [com.tunjid.heron.data.ml.model.InferenceModel] whose
 * [com.tunjid.heron.data.ml.model.InferenceModel.minDeviceMemoryInGb] exceeds what the device
 * can comfortably hold. Reads total RAM rather than currently-available RAM: the model outlives
 * any momentary free-memory reading, so the ceiling that matters is the hardware's.
 */
@Composable
internal expect fun rememberPlatformMemoryBytes(): Long
