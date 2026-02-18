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

package com.tunjid.heron.gallery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.gallery.GalleryItem
import com.tunjid.heron.images.DownloadStatus
import com.tunjid.heron.images.ImageRequest
import com.tunjid.heron.images.LocalImageLoader
import heron.feature.gallery.generated.resources.Res
import heron.feature.gallery.generated.resources.download
import heron.feature.gallery.generated.resources.download_complete
import heron.feature.gallery.generated.resources.download_failed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ImageDownloadState.DownloadButton(
    item: GalleryItem.Media.Photo,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalImageLoader.current
    val coroutineScope = rememberCoroutineScope()
    val downloadStatusState = stateFor(item)

    Box(modifier = modifier) {
        val contentModifier = Modifier.align(Alignment.Center).size(40.dp)

        val onDownloadClicked: () -> Unit =
            remember(item.image.fullsize) {
                {
                    coroutineScope.launch {
                        imageLoader
                            .download(ImageRequest.Network(item.image.fullsize.uri))
                            .collectLatest { updateStateFor(item, it) }
                    }
                }
            }

        AnimatedContent(
            targetState = downloadStatusState,
            contentKey = DownloadStatus?::contentKey,
        ) { status ->
            when (status) {
                DownloadStatus.Complete ->
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(Res.string.download_complete),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = contentModifier,
                    )
                DownloadStatus.Failed ->
                    IconButton(
                        modifier = contentModifier,
                        onClick = { updateStateFor(item, null) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = stringResource(Res.string.download_failed),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = contentModifier,
                        )
                    }
                DownloadStatus.Indeterminate ->
                    CircularProgressIndicator(modifier = contentModifier)
                is DownloadStatus.Progress ->
                    CircularProgressIndicator(
                        modifier = contentModifier,
                        // let is needed bc of compose lint about method references
                        progress =
                            animateFloatAsState(status.fraction).let { state -> state::value },
                    )
                null ->
                    IconButton(modifier = contentModifier, onClick = onDownloadClicked) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(Res.string.download),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = contentModifier,
                        )
                    }
            }
        }
    }
}

private val DownloadStatus?.contentKey
    get() =
        when (this) {
            null -> "-"
            else -> this::class.simpleName
        }

internal class ImageDownloadState {
    private val states = mutableStateMapOf<String, DownloadStatus?>()

    fun stateFor(photo: GalleryItem.Media.Photo): DownloadStatus? = states[photo.image.fullsize.uri]

    fun updateStateFor(photo: GalleryItem.Media.Photo, status: DownloadStatus?) {
        states[photo.image.fullsize.uri] = status
    }
}
