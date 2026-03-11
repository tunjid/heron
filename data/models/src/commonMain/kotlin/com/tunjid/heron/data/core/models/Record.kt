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

import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.RecordUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An AtProto record
 */
@Serializable
sealed interface Record {

    val reference: Reference

    /**
     * An AtProto record that may be embedded in a [Post]
     */
    @Serializable
    // This interface used to be called Post.
    // Preserve its existing name for backwards compatibility
    @SerialName("com.tunjid.heron.data.core.models.Record.Post")
    sealed interface Embeddable : Record {
        val embeddableRecordUri: EmbeddableRecordUri
    }

    @Serializable
    data class Reference(
        val id: Id?,
        val uri: RecordUri,
    )
}
