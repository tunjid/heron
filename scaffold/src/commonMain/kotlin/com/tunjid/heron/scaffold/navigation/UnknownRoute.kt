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

package com.tunjid.heron.scaffold.navigation

import com.tunjid.treenav.strings.RouteParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializedRouteParams = @Serializable(RouteParamsSerializer::class) RouteParams

@Serializable
@SerialName("RouteParams")
private class RouteParamsSurrogate(
    val route: String,
    val pathArgs: Map<String, String>,
    val queryParams: Map<String, List<String>>,
)

internal object RouteParamsSerializer : KSerializer<RouteParams> {
    override val descriptor: SerialDescriptor = RouteParamsSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RouteParams) {
        val surrogate = RouteParamsSurrogate(
            route = value.pathAndQueries,
            pathArgs = value.pathArgs,
            queryParams = value.queryParams
        )
        encoder.encodeSerializableValue(RouteParamsSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): RouteParams {
        val surrogate = decoder.decodeSerializableValue(RouteParamsSurrogate.serializer())
        return RouteParams(
            pathAndQueries = surrogate.route,
            pathArgs = surrogate.pathArgs,
            queryParams = surrogate.queryParams
        )
    }
}
