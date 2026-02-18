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

interface OauthFlowState {
    val supportsOauth: Boolean

    fun launch(uri: GenericUri)
}

sealed class OauthFlowResult {
    data class Success(val callbackUri: GenericUri, val issuer: String) : OauthFlowResult()

    data object Failure : OauthFlowResult()
}

@Composable expect fun rememberOauthFlowState(onResult: (OauthFlowResult) -> Unit): OauthFlowState
