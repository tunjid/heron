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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tunjid.heron.data.core.types.GenericUri
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionCallback
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberOauthFlowState(
    onResult: (OauthFlowResult) -> Unit,
): OauthFlowState {
    val currentOnResult by rememberUpdatedState(onResult)
    val provider = remember { PresentationProvider() }
    val state = remember(provider) {
        AsWebAuthOauthFlowState(
            presentationContextProvider = provider,
            onResult = { currentOnResult(it) },
        )
    }
    DisposableEffect(state) {
        onDispose { state.cancel() }
    }
    return state
}

private const val OauthIssuerKey = "iss"
private const val OauthCallbackHost = "heron.tunji.dev"
private const val OauthCallbackPath = "/oauth/callback"

@OptIn(ExperimentalForeignApi::class)
private class AsWebAuthOauthFlowState(
    private val presentationContextProvider: ASWebAuthenticationPresentationContextProvidingProtocol,
    private val onResult: (OauthFlowResult) -> Unit,
) : OauthFlowState {

    private var session: ASWebAuthenticationSession? = null

    override val supportsOauth: Boolean = true

    override fun launch(uri: GenericUri) {
        session?.cancel()
        val url = NSURL(string = uri.uri)
        val callback = ASWebAuthenticationSessionCallback.callbackWithHTTPSHost(
            host = OauthCallbackHost,
            path = OauthCallbackPath,
        )
        val newSession = ASWebAuthenticationSession(
            uRL = url,
            callback = callback,
            completionHandler = { callbackUrl, error ->
                val result = parseResult(callbackUrl, error)
                platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                    onResult(result)
                }
            },
        )
        newSession.presentationContextProvider = presentationContextProvider
        session = newSession
        newSession.start()
    }

    fun cancel() {
        session?.cancel()
        session = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun parseResult(callbackUrl: NSURL?, error: NSError?): OauthFlowResult {
    if (error != null || callbackUrl == null) return OauthFlowResult.Failure
    val components = NSURLComponents(uRL = callbackUrl, resolvingAgainstBaseURL = false)
    val issuer = components.queryItems
        ?.asSequence()
        ?.mapNotNull { it as? NSURLQueryItem }
        ?.firstOrNull { it.name == OauthIssuerKey }
        ?.value
        ?: return OauthFlowResult.Failure
    val absolute = callbackUrl.absoluteString ?: return OauthFlowResult.Failure
    return OauthFlowResult.Success(
        callbackUri = GenericUri(absolute),
        issuer = issuer,
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class PresentationProvider :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {

    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): ASPresentationAnchor =
        UIApplication.sharedApplication
            .connectedScenes
            .asSequence()
            .mapNotNull { it as? UIWindowScene }
            .flatMap { scene ->
                scene.windows.asSequence().mapNotNull { it as? UIWindow }
            }
            .first(UIWindow::isKeyWindow)
}
