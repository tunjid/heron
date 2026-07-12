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

package com.tunjid.heron.data.platform

import kotlinx.coroutines.flow.Flow

enum class MemoryPressure {
    Normal,
    Elevated,
    Critical,
}

interface MemoryMonitor {
    /** Total physical RAM in bytes; constant for the device, `0` if it can't be determined. */
    val totalMemoryBytes: Long

    val pressure: Flow<MemoryPressure>
}
