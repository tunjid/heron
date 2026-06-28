package com.tunjid.heron.images

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class StubImage : Image {
    override val size: IntSize
        get() = IntSize.Zero

    override val painter: Painter = ColorPainter(Color.LightGray)
}

val StubImageLoader = object : ImageLoader {
    override suspend fun fetchImage(
        request: ImageRequest,
        size: IntSize,
    ): Image = StubImage()

    override fun download(
        request: ImageRequest.Network,
    ): Flow<DownloadStatus> = flowOf(DownloadStatus.Failed)
}
