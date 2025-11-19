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

package com.tunjid.heron.data.core.utilities

import com.tunjid.heron.data.core.models.Link
import kotlinx.serialization.Serializable

@Serializable
data class TextInputSnapshot(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    // Composition is nullable in TextFieldValue. We use -1 to represent null.
    val compositionStart: Int = -1,
    val compositionEnd: Int = -1,
    val links: List<Link> = emptyList(),
)
