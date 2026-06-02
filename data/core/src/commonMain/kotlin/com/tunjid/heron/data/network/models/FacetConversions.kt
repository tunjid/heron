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

package com.tunjid.heron.data.network.models

import app.bsky.richtext.Facet
import app.bsky.richtext.FacetFeatureUnion
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId

internal fun Facet.toLinkOrNull(): Link? {
    return if (features.isEmpty()) null
    else
        Link(
            start = index.byteStart.toInt(),
            end = index.byteEnd.toInt(),
            target =
                when (val feature = features.first()) {
                    is FacetFeatureUnion.Link ->
                        LinkTarget.ExternalLink(feature.value.uri.uri.let(::GenericUri))
                    is FacetFeatureUnion.Mention ->
                        LinkTarget.UserDidMention(feature.value.did.did.let(::ProfileId))

                    is FacetFeatureUnion.Tag -> LinkTarget.Hashtag(feature.value.tag)
                    is FacetFeatureUnion.Unknown -> return null
                },
        )
}
