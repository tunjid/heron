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

package com.tunjid.heron.signin.oauth

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.types.GenericUri
import java.net.URI

@Composable
actual fun rememberOauthFlowState(onResult: (OauthFlowResult) -> Unit): OauthFlowState =
    LoopbackOauthFlowState

private object LoopbackOauthFlowState : OauthFlowState {

    override val supportsOauth: Boolean = true

    override fun launch(uri: GenericUri) {
        // Auth callback is handled in the data layer via loopback server.
        // This just opens the system browser for the user to authenticate.
        java.awt.Desktop.getDesktop().browse(URI(uri.uri))
    }
}
