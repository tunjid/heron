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

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
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
    sealed interface Post : Record

    @Serializable
    data class Reference(
        val id: Id,
        val uri: RecordUri,
    )
}
