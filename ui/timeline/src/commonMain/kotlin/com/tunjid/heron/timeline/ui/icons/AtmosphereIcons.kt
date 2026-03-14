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

package com.tunjid.heron.timeline.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Public
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Server
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.at_proto
import heron.ui.timeline.generated.resources.blacksky_server
import heron.ui.timeline.generated.resources.bluesky_server
import heron.ui.timeline.generated.resources.custom_server
import heron.ui.timeline.generated.resources.eurosky_server
import heron.ui.timeline.generated.resources.pckt_server
import org.jetbrains.compose.resources.StringResource

data object AtmosphereIcons {
    val Bluesky: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Bluesky
    val Blacksky: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Blacksky
    val Eurosky: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Eurosky
    val Leaflet: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Leaflet
    val Pckt: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Pckt
    val Help: ImageVector get() = com.tunjid.heron.timeline.ui.icons.Help
}

val AtProtoServer = Server(
    endpoint = "https://atproto.com/",
    supportsOauth = false,
)

val Server.logo: ImageVector
    get() = when (endpoint) {
        Server.BlueSky.endpoint -> AtmosphereIcons.Bluesky
        Server.BlackSky.endpoint -> AtmosphereIcons.Blacksky
        Server.EuroSky.endpoint -> AtmosphereIcons.Eurosky
        Server.Pckt.endpoint -> AtmosphereIcons.Pckt
        AtProtoServer.endpoint -> Icons.Rounded.AlternateEmail
        else -> Icons.Rounded.Public
    }

val Server.stringResource: StringResource
    get() = when (endpoint) {
        Server.BlueSky.endpoint -> Res.string.bluesky_server
        Server.BlackSky.endpoint -> Res.string.blacksky_server
        Server.EuroSky.endpoint -> Res.string.eurosky_server
        Server.Pckt.endpoint -> Res.string.pckt_server
        AtProtoServer.endpoint -> Res.string.at_proto
        else -> Res.string.custom_server
    }
