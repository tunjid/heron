package com.tunjid.heron.media.video

import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun VideoSurface(
    modifier: Modifier,
    exoPlayer: ExoPlayer?,
    contentScale: ContentScale,
    alignment: Alignment,
    shape: Shape,
    videoSize: IntSize,
) {
    val updatedPlayer = rememberUpdatedState(exoPlayer)
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val videoMatrix by remember {
        mutableStateOf(
            value = Matrix(),
            policy = referentialEqualityPolicy(),
        )
    }

    AndroidEmbeddedExternalSurface(
        modifier = modifier
            .clip(shape)
            .onSizeChanged { surfaceSize = it },
        transform = videoMatrix,
    ) {
        onSurface { createdSurface, initialWidth, initialHeight ->
            surfaceSize = IntSize(initialWidth, initialHeight)
            createdSurface.onChanged { width, height ->
                surfaceSize = IntSize(width, height)
            }
            createdSurface.onDestroyed {
                updatedPlayer.value?.setVideoSurface(null)
            }
            snapshotFlow { updatedPlayer.value }
                .filterNotNull()
                .collectLatest {
                    it.setVideoSurface(createdSurface)
                }
        }
    }

    if (videoSize.height != 0 && videoSize.width != 0 && surfaceSize.height != 0 && surfaceSize.width != 0) {
        // Recalculate the video matrix
        videoMatrix.scaleAndAlignTo(
            srcSize = videoSize,
            destSize = surfaceSize,
            contentScale = contentScale,
            alignment = alignment,
        )
    }
}

/**
 * Scales and aligns a matrix into [destSize] from [srcSize].
 */
private fun Matrix.scaleAndAlignTo(
    srcSize: IntSize,
    destSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
) = apply {
    // Reset the matrix to identity
    reset()
    // TextureView defaults to Fill bounds, first remove that transform
    val fillBoundsScaleFactor = ContentScale.FillBounds.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize(),
    )
    scale(
        x = fillBoundsScaleFactor.scaleX,
        y = fillBoundsScaleFactor.scaleY,
    )
    // Remove default fill bounds by inverting the matrix
    invert()

    // Next apply the desired contentScale
    val desiredScaleFactor = contentScale.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize(),
    )
    scale(
        x = desiredScaleFactor.scaleX,
        y = desiredScaleFactor.scaleY,
    )

    // Finally apply the desired alignment
    val scaledSrcSize = srcSize.toSize() * desiredScaleFactor

    val alignmentOffset = alignment.align(
        size = IntSize(
            width = scaledSrcSize.width.toInt(),
            height = scaledSrcSize.height.toInt(),
        ),
        space = destSize,
        layoutDirection = LayoutDirection.Ltr,
    )

    // Translate by the alignment, taking into account the desired scale factor and
    // the implicit fill bounds.
    val translationOffset = Offset(
        x = alignmentOffset.x / desiredScaleFactor.scaleX * fillBoundsScaleFactor.scaleX,
        y = alignmentOffset.y / desiredScaleFactor.scaleY * fillBoundsScaleFactor.scaleY,
    )

    translate(
        x = translationOffset.x,
        y = translationOffset.y,
    )
}
