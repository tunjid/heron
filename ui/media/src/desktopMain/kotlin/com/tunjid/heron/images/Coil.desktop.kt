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

package com.tunjid.heron.images

import androidx.compose.runtime.Composable
import coil3.Image
import coil3.PlatformContext
import io.github.vinceglb.filekit.FileKit

fun imageLoader(): ImageLoader = CoilImageLoader.create(
    context = PlatformContext.INSTANCE,
).also { FileKit.init(appId = AppId) }

@Composable
internal actual fun Image.AnimationEffect() = Unit

private const val AppId = "com.tunjid.heron"
