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

package com.tunjid.heron.ui.scaffold.navigation

import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeQuery
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val Route.sharedUri: GenericUri? by optionalMappedRouteQuery(
    mapper = ::GenericUri,
)

inline fun <reified T> Route.model(): T? = models.asSequence()
    .filterIsInstance<T>()
    .firstOrNull()

val Route.models: List<UrlEncodableModel>
    get() = routeParams.queryParams["model"]
        ?.map(String::fromBase64EncodedUrl)
        ?: emptyList()

val Route.avatarSharedElementKey by optionalRouteQuery()

@OptIn(ExperimentalUuidApi::class)
val Route.sharedElementPrefix by routeQuery(
    default = Uuid.random().toHexString(),
)
