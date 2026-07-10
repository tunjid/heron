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
import platform.Foundation.NSProcessInfo

@Composable
internal actual fun rememberPlatformMemoryBytes(): Long = remember {
    // physicalMemory is total device RAM. iOS also caps per-process memory well below this via
    // jetsam, so this over-states what a model can actually claim; it stays a coarse floor check.
    NSProcessInfo.processInfo.physicalMemory.toLong()
}
