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

import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostReferencingRecordUri
import com.tunjid.heron.data.core.types.PostUri
import kotlinx.serialization.Serializable

@Serializable
sealed class RecordPostReference {
    abstract val recordUri: PostReferencingRecordUri

    @Serializable
    data class Link(
        override val recordUri: PostReferencingRecordUri,
        val postUri: PostUri,
        val postCid: PostId,
    ) : RecordPostReference()

    @Serializable
    data class Unlink(
        override val recordUri: PostReferencingRecordUri,
    ) : RecordPostReference()
}

fun StandardDocument.PostReference.asRecordPostReference(): RecordPostReference =
    when (this) {
        is StandardDocument.PostReference.Link -> RecordPostReference.Link(
            recordUri = documentUri,
            postUri = postUri,
            postCid = postCid,
        )
        is StandardDocument.PostReference.Unlink -> RecordPostReference.Unlink(
            recordUri = documentUri,
        )
    }
