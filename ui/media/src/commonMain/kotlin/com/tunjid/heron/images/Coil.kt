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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.asPainter
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest as CoilImageRequest
import coil3.size.Size as CoilSize
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.saveImageToGallery
import io.github.vinceglb.filekit.sink
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.io.buffered

@Immutable
internal class CoilImage(
    val image: coil3.Image,
    override val painter: Painter,
) : Image {

    override val size: IntSize
        get() = IntSize(
            width = image.width,
            height = image.height,
        )
}

internal class CoilImageLoader private constructor(
    private val platformContext: PlatformContext,
    private val httpClient: HttpClient,
) : ImageLoader {

    override suspend fun fetchImage(
        request: ImageRequest,
        size: IntSize,
    ): Image? {
        val coilImageLoader = SingletonImageLoader.get(platformContext)
        val coilRequest = CoilImageRequest.Builder(platformContext).apply {
            when (request) {
                is ImageRequest.Local -> {
                    data(request.file)
                }
                is ImageRequest.Network -> {
                    data(request.url)
                    request.thumbnailUrl
                        ?.let(MemoryCache::Key)
                        ?.let { cacheKey ->
                            placeholder {
                                coilImageLoader
                                    .memoryCache
                                    ?.get(cacheKey)
                                    ?.image
                            }
                        }
                    // TODO: This is only done for network images for now. This is because
                    // Local images need to be loaded as is to obtain the proper dimensions
                    size(
                        CoilSize(
                            width = size.width,
                            height = size.height,
                        ),
                    )
                }
            }
        }
            .build()

        val image = coilImageLoader.execute(coilRequest).image ?: return null

        return CoilImage(
            image = image,
            painter = image.asPainter(platformContext),
        )
    }

    override fun download(
        request: ImageRequest.Network,
    ): Flow<DownloadStatus> = when (val url = request.url) {
        null -> emptyFlow()
        else -> flow {
            var tempFile: PlatformFile? = null
            try {
                val fileName = "${Clock.System.now().toEpochMilliseconds()}.jpg"
                if (!MediaDownloadsDir.exists()) MediaDownloadsDir.createDirectories(true)
                val file = (MediaDownloadsDir / fileName).also { tempFile = it }

                val sink = file.sink(append = false)

                httpClient.prepareGet(url).execute { httpResponse ->
                    if (httpResponse.status != HttpStatusCode.OK) {
                        emit(DownloadStatus.Failed)
                        return@execute
                    }

                    val channel: ByteReadChannel = httpResponse.body()
                    var count = 0L
                    sink.buffered().use { bufferedSink ->
                        val buffer = ByteArray(DownloadBufferSize)
                        while (true) {
                            val read = channel.readAvailable(buffer)
                            if (read <= 0) break
                            bufferedSink.write(buffer, 0, read)
                            count += read

                            val contentLength = httpResponse.contentLength()
                            emit(
                                if (contentLength == null) DownloadStatus.Indeterminate
                                else DownloadStatus.Progress(count.toFloat() / contentLength),
                            )
                        }
                    }
                }
                FileKit.saveImageToGallery(file)
                emit(DownloadStatus.Complete)
            } catch (e: Exception) {
                emit(DownloadStatus.Failed)
            } finally {
                tempFile?.delete()
            }
        }
    }

    companion object {
        internal fun create(
            context: PlatformContext,
        ): ImageLoader {
            val httpClient = HttpClient()
            SingletonImageLoader.setSafe {
                coil3.ImageLoader.Builder(context)
                    .components {
                        addPlatformFileSupport()
                        add(KtorNetworkFetcherFactory(httpClient))
                    }
                    .build()
            }
            return CoilImageLoader(
                platformContext = context,
                httpClient = httpClient,
            )
        }
    }
}

@Composable
internal expect fun coil3.Image.AnimationEffect()

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> {
    throw IllegalArgumentException("Image Fetcher has not been provided")
}

private val MediaDownloadsDir = FileKit.cacheDir / "media-downloads"
private const val DownloadBufferSize = 8192
