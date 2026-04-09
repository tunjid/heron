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

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.ui.icons.AlternateEmail
import com.tunjid.heron.ui.icons.Blacksky
import com.tunjid.heron.ui.icons.Bluesky
import com.tunjid.heron.ui.icons.Eurosky
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.Pckt
import com.tunjid.heron.ui.icons.Public
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.at_proto
import heron.ui.timeline.generated.resources.blacksky_server
import heron.ui.timeline.generated.resources.bluesky_server
import heron.ui.timeline.generated.resources.custom_server
import heron.ui.timeline.generated.resources.eurosky_server
import heron.ui.timeline.generated.resources.pckt_server
import org.jetbrains.compose.resources.StringResource

val AtProtoServer = Server(
    endpoint = "https://atproto.com/",
    supportsOauth = false,
)

val Server.logo: ImageVector
    get() = when (endpoint) {
        Server.BlueSky.endpoint -> HeronIcons.Atmospheric.Bluesky
        Server.BlackSky.endpoint -> HeronIcons.Atmospheric.Blacksky
        Server.EuroSky.endpoint -> HeronIcons.Atmospheric.Eurosky
        Server.Pckt.endpoint -> HeronIcons.Atmospheric.Pckt
        AtProtoServer.endpoint -> HeronIcons.AlternateEmail
        else -> HeronIcons.Public
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
