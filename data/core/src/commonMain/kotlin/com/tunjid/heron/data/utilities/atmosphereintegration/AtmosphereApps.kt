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

package com.tunjid.heron.data.utilities.atmosphereintegration

import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.TrackUri

val SupportedAtmosphereApps: List<AtmosphereApp> = listOf(
    AtmosphereApp(
        id = AtmosphereApp.StandardSiteId,
        webpage = GenericUri("https://standard.site"),
        logo = ImageUri("https://standard.site/favicon.ico"),
    ),
    AtmosphereApp(
        id = AtmosphereApp.RockskyId,
        webpage = GenericUri("https://rocksky.app"),
        logo = ImageUri("https://rocksky.app/favicon.ico"),
    ),
)

internal val AtmosphereAppNsids: Map<String, List<String>> = mapOf(
    AtmosphereApp.StandardSiteId to listOf(
        StandardPublicationUri.NAMESPACE,
        StandardDocumentUri.NAMESPACE,
    ),
    AtmosphereApp.RockskyId to listOf(
        ScrobbleUri.NAMESPACE,
        TrackUri.NAMESPACE,
        AlbumUri.NAMESPACE,
        ArtistUri.NAMESPACE,
    ),
)
