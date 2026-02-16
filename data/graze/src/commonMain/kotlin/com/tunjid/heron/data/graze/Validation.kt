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

package com.tunjid.heron.data.graze

val Filter.isValid: Boolean
    get() = when (this) {
        is Filter.Root -> filters.all(Filter::isValid)
        is Filter.Attribute.Compare -> targetValue.isNotBlank()
        is Filter.Attribute.Embed -> true
        is Filter.Entity.Matches -> values.isNotEmpty()
        is Filter.Entity.Excludes -> values.isNotEmpty()
        is Filter.Regex.Matches -> pattern.isNotEmpty()
        is Filter.Regex.Negation -> pattern.isNotEmpty()
        is Filter.Regex.Any -> terms.isNotEmpty()
        is Filter.Regex.None -> terms.isNotEmpty()
        is Filter.Social.Graph -> username.isNotBlank()
        is Filter.Social.UserList -> dids.isNotEmpty()
        is Filter.Social.StarterPack -> url.isNotBlank()
        is Filter.Social.ListMember -> url.isNotBlank()
        is Filter.Social.MagicAudience -> audienceId.isNotBlank()
        is Filter.ML.Similarity -> path.isNotBlank()
        is Filter.ML.Probability -> config.modelName.isNotBlank()
        is Filter.ML.Moderation -> true
        is Filter.Analysis -> true
    }
