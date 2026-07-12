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

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification

internal class IosMemoryMonitor : MemoryMonitor {

    // physicalMemory is total device RAM. iOS also caps per-process memory well below this via
    // jetsam, so this over-states what a model can actually claim; it stays a coarse floor.
    override val totalMemoryBytes: Long =
        NSProcessInfo.processInfo.physicalMemory.toLong()

    // UIKit only signals a single "memory warning"; treat it as Critical. It carries no recovery
    // ("back to normal") event, which suits shedding the model rather than tracking a level.
    override val pressure: Flow<MemoryPressure> = callbackFlow {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidReceiveMemoryWarningNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            trySend(MemoryPressure.Critical)
        }
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
}

fun createMemoryMonitor(): MemoryMonitor = IosMemoryMonitor()
