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

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class JvmMemoryMonitor(
    ioDispatcher: CoroutineDispatcher,
) : MemoryMonitor {

    private val osBean: OperatingSystemMXBean? =
        ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean

    // The model loads into native memory, so total physical RAM is the ceiling, not the JVM heap.
    override val totalMemoryBytes: Long =
        osBean?.totalMemorySize ?: 0L

    // The JVM exposes no OS memory-pressure push, so poll free physical RAM. The weakest of the
    // three platforms — desktops swap — but real. distinctUntilChanged only emits level crossings.
    override val pressure: Flow<MemoryPressure> = flow {
        val bean = osBean ?: return@flow
        while (true) {
            val total = bean.totalMemorySize.toDouble()
            val freeFraction = if (total > 0) bean.freeMemorySize / total else 1.0
            emit(
                when {
                    freeFraction < 0.05 -> MemoryPressure.Critical
                    freeFraction < 0.15 -> MemoryPressure.Elevated
                    else -> MemoryPressure.Normal
                },
            )
            delay(3.seconds)
        }
    }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .distinctUntilChanged()
}

fun createMemoryMonitor(
    ioDispatcher: CoroutineDispatcher,
): MemoryMonitor = JvmMemoryMonitor(
    ioDispatcher = ioDispatcher,
)
