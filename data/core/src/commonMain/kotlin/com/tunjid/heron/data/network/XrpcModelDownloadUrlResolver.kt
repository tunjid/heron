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

package com.tunjid.heron.data.network

import com.tunjid.heron.data.ml.model.ModelDownloadUrlResolver
import dev.tunji.heron.GetModelUrlQueryParams
import dev.zacsweers.metro.Inject

/**
 * Resolves signed model download URLs by calling the `dev.tunji.heron.getModelUrl` XRPC query on the
 * heron appview. The call is routed and service-authed through the atproto proxy (its NSID is
 * registered in the session manager's heron proxy paths).
 */
@Inject
internal class XrpcModelDownloadUrlResolver(
    private val networkService: NetworkService,
) : ModelDownloadUrlResolver {

    override suspend fun resolve(
        bucketPath: String,
    ): String? = networkService.runCatchingWithMonitoredNetworkRetry {
        getModelUrl(
            GetModelUrlQueryParams(
                path = bucketPath,
            ),
        )
    }
        .getOrNull()
        ?.url
        ?.uri
}
