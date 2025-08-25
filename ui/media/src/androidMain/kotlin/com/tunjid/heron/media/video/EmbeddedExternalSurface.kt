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

package com.tunjid.heron.media.video

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.SurfaceCoroutineScope
import androidx.compose.foundation.SurfaceScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * A copy of AndroidExternalSurface with null checks for surface callbacks
 */
@Composable
internal fun EmbeddedExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    transform: Matrix? = null,
    onInit: AndroidExternalSurfaceScope.() -> Unit
) {
    val state = rememberEmbeddedExternalSurfaceState()

    AndroidView(
        factory = { TextureView(it) },
        modifier = modifier,
        onReset = {},
        update = { view ->
            if (surfaceSize != IntSize.Zero) {
                view.surfaceTexture?.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
            }
            state.surfaceSize = surfaceSize
            if (view.surfaceTextureListener !== state) {
                state.onInit()
                view.surfaceTextureListener = state
            }
            view.isOpaque = isOpaque
            // If transform is null, we'll call setTransform(null) which sets the
            // identity transform on the TextureView
            view.setTransform(transform?.let { state.matrix.apply { setFrom(transform) } })
        }
    )
}

@Composable
private fun rememberEmbeddedExternalSurfaceState(): EmbeddedExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { EmbeddedExternalSurfaceState(scope) }
}

private class EmbeddedExternalSurfaceState(scope: CoroutineScope) :
    BaseAndroidExternalSurfaceState(scope), TextureView.SurfaceTextureListener {

    var surfaceSize = IntSize.Zero
    val matrix = android.graphics.Matrix()

    private var surfaceTextureSurface: Surface? = null

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        var w = width
        var h = height

        if (surfaceSize != IntSize.Zero) {
            w = surfaceSize.width
            h = surfaceSize.height
            surfaceTexture.setDefaultBufferSize(w, h)
        }

        val surface = Surface(surfaceTexture)
        surfaceTextureSurface = surface

        dispatchSurfaceCreated(surface, w, h)
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        var w = width
        var h = height

        if (surfaceSize != IntSize.Zero) {
            w = surfaceSize.width
            h = surfaceSize.height
            surfaceTexture.setDefaultBufferSize(w, h)
        }

        surfaceTextureSurface?.let { dispatchSurfaceChanged(it, w, h) }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        surfaceTextureSurface?.let(::dispatchSurfaceDestroyed)
        surfaceTextureSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // onSurfaceTextureUpdated is called when the content of the SurfaceTexture
        // has changed, which is not relevant to us since we are the producer here
    }
}

private abstract class BaseAndroidExternalSurfaceState(val scope: CoroutineScope) :
    androidx.compose.foundation.AndroidExternalSurfaceScope, SurfaceScope {

    private var onSurface:
            (suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit)? =
        null
    private var onSurfaceChanged: (Surface.(width: Int, height: Int) -> Unit)? = null
    private var onSurfaceDestroyed: (Surface.() -> Unit)? = null

    private var job: Job? = null

    override fun onSurface(
        onSurface: suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit
    ) {
        this.onSurface = onSurface
    }

    override fun Surface.onChanged(onChanged: Surface.(width: Int, height: Int) -> Unit) {
        onSurfaceChanged = onChanged
    }

    override fun Surface.onDestroyed(onDestroyed: Surface.() -> Unit) {
        onSurfaceDestroyed = onDestroyed
    }

    /**
     * Dispatch a surface creation event by launching a new coroutine in [scope]. Any previous job
     * from a previous surface creation dispatch is cancelled.
     */
    fun dispatchSurfaceCreated(surface: Surface, width: Int, height: Int) {
        if (onSurface != null) {
            job =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    job?.cancelAndJoin()
                    val receiver =
                        object :
                            SurfaceCoroutineScope,
                            SurfaceScope by this@BaseAndroidExternalSurfaceState,
                            CoroutineScope by this {}
                    onSurface?.invoke(receiver, surface, width, height)
                }
        }
    }

    /**
     * Dispatch a surface change event, providing the surface's new width and height. Must be
     * invoked from the main thread.
     */
    fun dispatchSurfaceChanged(surface: Surface, width: Int, height: Int) {
        onSurfaceChanged?.invoke(surface, width, height)
    }

    /**
     * Dispatch a surface destruction event. Any pending job from [dispatchSurfaceCreated] is
     * cancelled before dispatching the event. Must be invoked from the main thread.
     */
    fun dispatchSurfaceDestroyed(surface: Surface) {
        onSurfaceDestroyed?.invoke(surface)
        job?.cancel()
        job = null
    }
}