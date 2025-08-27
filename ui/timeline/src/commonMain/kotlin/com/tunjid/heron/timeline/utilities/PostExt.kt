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

package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Post

val Post.createdAt get() = record?.createdAt ?: indexedAt

val Map<Label.Visibility, List<Label.Definition>>.blurredMediaDefinitions
    get(): List<Label.Definition> = getOrElse(
        key = Label.Visibility.Warn,
        defaultValue = ::emptyList,
    ).filter(Label.Definition::blursMedia) + getOrElse(
        key = Label.Visibility.Hide,
        defaultValue = ::emptyList,
    ).filter(Label.Definition::blursMedia)

val Map<Label.Visibility, List<Label.Definition>>.canAutoPlayVideo
    get(): Boolean = getOrElse(
        key = Label.Visibility.Warn,
        defaultValue = ::emptyList,
    ).none(Label.Definition::blursMedia) && getOrElse(
        key = Label.Visibility.Hide,
        defaultValue = ::emptyList,
    ).none(Label.Definition::blursMedia)

val Label.Definition.blursMedia
    get() = adultOnly || blurs == Label.BlurTarget.Media
