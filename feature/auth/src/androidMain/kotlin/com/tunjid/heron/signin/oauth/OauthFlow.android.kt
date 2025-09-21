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

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.tunjid.heron.data.core.types.GenericUri

@Composable
actual fun rememberOauthFlowState(
    onResult: (OauthFlowResult) -> Unit,
): OauthFlowState {
    val launcher = rememberLauncherForActivityResult(
        contract = AtProtoOauthContract(),
        onResult = onResult,
    )

    return remember(launcher) {
        ChromeAuthTabOauthFlowState(
            activityResultLauncher = launcher,
        )
    }
}

private class ChromeAuthTabOauthFlowState(
    private val activityResultLauncher: ActivityResultLauncher<GenericUri>,
) : OauthFlowState {

    override val supportsOauth: Boolean
        get() = true

    override fun launch(uri: GenericUri) =
        activityResultLauncher.launch(uri)
}

/**
 * An ActivityResultContract to launch a Chrome Custom Tab and parse the result.
 *
 * This contract takes a URL string as input and returns the redirected URL string
 * as output, which is received in the onNewIntent of the handling activity.
 */
private class AtProtoOauthContract : ActivityResultContract<GenericUri, OauthFlowResult>() {

    /**
     * Creates an Intent to launch a Chrome Custom Tab.
     *
     * @param context The context to use for creating the intent.
     * @param input The URL to open in the Custom Tab.
     * @return An Intent configured to launch a Custom Tab with the specified URL.
     */
    override fun createIntent(
        context: Context,
        input: GenericUri,
    ): Intent {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        // We are using the Custom Tab's Intent to ensure the browser redirect
        // can be caught by our app.
        customTabsIntent.intent.data = input.uri.toUri()
        return customTabsIntent.intent
    }

    /**
     * Parses the result from the Activity.
     *
     * In this OAuth flow, the result is delivered via a new Intent to the Activity,
     * not through the standard onActivityResult. Therefore, we expect the intent
     * from onNewIntent to be passed here.
     *
     * @param resultCode The result code from the activity (not typically used in this flow).
     * @param intent The intent received by the activity, which should contain the redirect URI.
     * @return The data string from the intent, which is our callback URL, or null if it's missing.
     */
    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): OauthFlowResult = when (resultCode) {
        Activity.RESULT_OK -> when (val callbackUri = intent?.data) {
            null -> OauthFlowResult.Failure
            else -> when(val issuer = intent.data?.getQueryParameter(OauthIssuerKey)) {
                null -> OauthFlowResult.Failure
                else -> OauthFlowResult.Success(
                    callbackUri = GenericUri(callbackUri.toString()),
                    issuer = issuer,
                )
            }
        }
        else -> OauthFlowResult.Failure
    }
}

private const val OauthIssuerKey = "iss"
