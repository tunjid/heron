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

import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StarterPack(
    val cid: StarterPackId,
    val uri: StarterPackUri,
    val name: String,
    val description: String?,
    val creator: Profile,
    val list: FeedList?,
    val joinedWeekCount: Long?,
    val joinedAllTimeCount: Long?,
    val indexedAt: Instant,
    val labels: List<Label>,
) : UrlEncodableModel