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

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class AndroidMemoryMonitor(
    private val context: Context,
) : MemoryMonitor {

    override val totalMemoryBytes: Long = run {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@run 0L
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        // API 34+ exposes advertised (marketed) RAM, closer to the number users recognise than
        // totalMem, which the kernel reports a little short of the hardware.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) memoryInfo.advertisedMem
        else memoryInfo.totalMem
    }

    override val pressure: Flow<MemoryPressure> = callbackFlow {
        val callbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                trySend(level.toMemoryPressure())
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in API 34, but still delivered on-device.")
            override fun onLowMemory() {
                trySend(MemoryPressure.Critical)
            }
        }
        context.registerComponentCallbacks(callbacks)
        awaitClose { context.unregisterComponentCallbacks(callbacks) }
    }
}

// The RUNNING_* levels fire while the app is foreground — the case that matters when a video is
// stressing memory. The constants are deprecated in API 34 but still delivered.
@Suppress("DEPRECATION")
private fun Int.toMemoryPressure(): MemoryPressure = when (this) {
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
    ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
    -> MemoryPressure.Critical

    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
    ComponentCallbacks2.TRIM_MEMORY_MODERATE,
    -> MemoryPressure.Elevated

    else -> MemoryPressure.Normal
}

fun createMemoryMonitor(
    context: Context,
): MemoryMonitor = AndroidMemoryMonitor(
    context = context,
)
