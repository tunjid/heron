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

package com.tunjid.heron.data.network

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class DPoPNonces {

    private val nonces = MutableStateFlow(emptyMap<String, String>())

    operator fun get(
        url: Url,
    ): String? = nonces.value[url.origin]

    fun update(
        url: Url,
        nonce: String?,
    ) {
        nonce ?: return
        val origin = url.origin
        if (nonces.value[origin] == nonce) return
        nonces.update { it + (origin to nonce) }
    }
}

private val Url.origin: String
    get() = "${protocol.name}://$host:$port"
