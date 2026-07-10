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
import androidx.compose.runtime.remember
import java.lang.management.ManagementFactory

@Composable
internal actual fun rememberPlatformMemoryBytes(): Long = remember {
    // The model loads into native memory, so total physical RAM is the ceiling that matters, not
    // the JVM heap (-Xmx). getTotalMemorySize() is available from Java 14+; the project targets 21.
    when (val bean = ManagementFactory.getOperatingSystemMXBean()) {
        is com.sun.management.OperatingSystemMXBean -> bean.totalMemorySize
        else -> 0L
    }
}
