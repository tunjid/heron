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

package com.tunjid.heron.data.files

import com.tunjid.heron.data.core.utilities.File
import okio.Path
import okio.Path.Companion.toPath

/**
 * Views this okio [Path] as a [File.System]. Preferred over the [File.System] constructor wherever a
 * [Path] is already in hand, so the `Path` ↔ `String` conversion stays in one place.
 */
fun Path.asSystemFile(): File.System =
    File.System(
        relativePath = toString(),
    )

/** The okio [Path] this [File.System] addresses. */
val File.System.path: Path
    get() = relativePath.toPath()
