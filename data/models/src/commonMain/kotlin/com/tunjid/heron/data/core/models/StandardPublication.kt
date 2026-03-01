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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import kotlinx.serialization.Serializable

@Serializable
data class StandardPublication(
    val uri: StandardPublicationUri,
    val name: String,
    val description: String?,
    val url: String,
    val icon: ImageUri?,
    val showInDiscover: Boolean,
    val basicTheme: BasicTheme?,
) : Record {
    override val reference: Record.Reference =
        Record.Reference(
            id = null,
            uri = uri,
        )

    @Serializable
    data class BasicTheme(
        val accent: ThemeColor,
        val accentForeground: ThemeColor,
        val background: ThemeColor,
        val foreground: ThemeColor,
    )

    @Serializable
    data class ThemeColor(
        val r: Int,
        val g: Int,
        val b: Int,
        val a: Int,
    )
}
