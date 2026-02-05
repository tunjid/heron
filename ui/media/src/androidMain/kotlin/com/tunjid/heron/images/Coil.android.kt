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

import android.content.Context
import android.graphics.drawable.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import coil3.DrawableImage

internal actual fun coil3.Image.renderInto(
    canvas: Canvas,
) = draw(canvas.nativeCanvas)

fun imageLoader(
    context: Context,
): ImageLoader = CoilImageLoader.create(
    context = context,
)

@Composable
internal actual fun coil3.Image.AnimationEffect() {
    DisposableEffect(this) {
        val animatable = (this@AnimationEffect as? DrawableImage)?.drawable as? Animatable
        animatable?.start()
        onDispose {
            animatable?.stop()
        }
    }
}
