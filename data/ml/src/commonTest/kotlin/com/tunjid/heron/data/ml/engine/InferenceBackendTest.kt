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

import kotlin.test.Test
import kotlin.test.assertEquals

class InferenceBackendTest {

    @Test
    fun cpuOnlyDevicesFallBackToCpu() {
        assertEquals(InferenceBackend.Cpu, backendFor(deviceModel = "Pixel 10"))
        // Matches the whole Pixel 10 line-up via substring, case-insensitively.
        assertEquals(InferenceBackend.Cpu, backendFor(deviceModel = "Pixel 10 Pro XL"))
        assertEquals(InferenceBackend.Cpu, backendFor(deviceModel = "pixel 10"))
    }

    @Test
    fun otherDevicesUseGpu() {
        assertEquals(InferenceBackend.Gpu, backendFor(deviceModel = "Pixel 9 Pro"))
        assertEquals(InferenceBackend.Gpu, backendFor(deviceModel = "SM-G998B"))
    }

    @Test
    fun unknownDeviceDefaultsToGpu() {
        assertEquals(InferenceBackend.Gpu, backendFor(deviceModel = null))
        assertEquals(InferenceBackend.Gpu, backendFor(deviceModel = ""))
    }
}
