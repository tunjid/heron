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

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Basic pagination query type, used for tiled requests.
 */
interface CursorQuery {
    val data: Data

    @Serializable
    data class Data(
        val page: Int,

        /**
         * The anchor point for the tiling pipeline.
         * Consecutive queries in a tiling pipeline mush have the same anchor unless
         * its being refreshed.
         */
        val cursorAnchor: Instant,

        /**
         * How many items to fetch for a query.
         */
        val limit: Long = 30L,
    ): UrlEncodableModel

    companion object {
        fun defaultStartData(
            limit: Long = 30L,
        ) = Data(
            page = 0,
            cursorAnchor = Clock.System.now(),
            limit = limit,
        )
    }
}

data class DataQuery(
    override val data: CursorQuery.Data,
) : CursorQuery

val CursorQuery.Data.offset get() = page * limit
