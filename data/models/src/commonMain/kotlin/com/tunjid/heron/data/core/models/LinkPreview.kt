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

import kotlinx.serialization.Serializable

/**
 * A pre-publish preview of the `app.bsky.embed.external` card a URL will resolve to, hydrated from
 * the cardyb `extract` endpoint.
 *
 * [records] holds the Atmosphere records that back the external view (when any), mirroring
 * [Post.embeddedRecords]: a `site.standard.*` URL surfaces both a [StandardDocument] and its
 * [StandardPublication].
 */
@Serializable
data class LinkPreview(
    val embed: ExternalEmbed,
    val records: List<Record.Embeddable.External> = emptyList(),
)

/**
 * The record to render alongside the [LinkPreview.embed], preferring the [StandardDocument] (which
 * nests its [StandardPublication]) — mirroring [Post.externalEmbeddedRecord].
 */
val LinkPreview.primaryRecord: Record.Embeddable.External?
    get() = records.firstOrNull { it is StandardDocument } ?: records.firstOrNull()
