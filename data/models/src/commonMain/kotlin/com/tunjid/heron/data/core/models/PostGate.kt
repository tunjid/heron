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

import com.tunjid.heron.data.core.types.PostGateUri
import com.tunjid.heron.data.core.types.PostUri
import kotlinx.serialization.Serializable

data class PostGate(
    val uri: PostGateUri,
    val gatedPostUri: PostUri,
    val allowed: AllowedEmbeds?,
) {
    @Serializable
    data class AllowedEmbeds(
        val none: Boolean = false,
    )

    companion object {
        val AllowedEmbeds?.embedsDisabled: Boolean
            get() = this != null && this.none
    }
}
