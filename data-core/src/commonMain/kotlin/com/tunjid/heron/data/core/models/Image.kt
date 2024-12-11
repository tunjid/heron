/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Uri

data class Image(
    val thumb: Uri,
    val fullsize: Uri,
    val alt: String,
    val width: Long?,
    val height: Long?,
)

val Image.aspectRatio get() =
    if(width != null && height != null) width.toFloat() / height
    else Float.NaN

data class ImageList(
    val images: List<Image>
):Embed
