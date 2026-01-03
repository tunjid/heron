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

package com.tunjid.heron.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.images.rememberUpdatedImageState
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import de.cketti.codepoints.codePointCount
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.alt_text
import heron.feature.compose.generated.resources.alt_text_add
import heron.feature.compose.generated.resources.alt_text_descriptive
import heron.ui.core.generated.resources.save
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

@Stable
class MediaAltTextSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    internal var media by mutableStateOf<RestrictedFile.Media?>(null)

    override fun onHidden() {
        media = null
    }

    fun editAltText(
        media: RestrictedFile.Media,
    ) {
        this.media = media
        show()
    }

    companion object {
        @Composable
        fun rememberMediaAltTextSheetState(
            onMediaItemUpdated: (RestrictedFile.Media) -> Unit,
        ): MediaAltTextSheetState {
            val state = rememberBottomSheetState {
                MediaAltTextSheetState(
                    scope = it,
                )
            }

            MediaAltTextBottomSheet(
                state = state,
                onMediaItemUpdated = onMediaItemUpdated,
            )

            return state
        }
    }
}

@Composable
private fun MediaAltTextBottomSheet(
    state: MediaAltTextSheetState,
    onMediaItemUpdated: (RestrictedFile.Media) -> Unit,
) {
    val media = state.media
    if (media != null) state.ModalBottomSheet {
        var altText by remember(media) {
            mutableStateOf(
                when (media) {
                    is RestrictedFile.Media.Photo -> media.altText
                    is RestrictedFile.Media.Video -> media.altText
                } ?: "",
            )
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.alt_text_add),
                style = MaterialTheme.typography.titleLarge,
            )

            when (media) {
                is RestrictedFile.Media.Photo -> {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        state = rememberUpdatedImageState(
                            args = ImageArgs(
                                item = media,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                shape = MediaUploadItemShape,
                            ),
                        ),
                    )
                }
                is RestrictedFile.Media.Video -> {
                    media.path?.let { videoPath ->
                        val videoPlayerController = LocalVideoPlayerController.current
                        val videoPlayerState =
                            videoPlayerController.rememberUpdatedVideoPlayerState(
                                videoUrl = videoPath,
                                thumbnail = null,
                                shape = MediaUploadItemShape,
                            )
                        VideoPlayer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            state = videoPlayerState,
                        )
                        LaunchedEffect(media) {
                            videoPlayerController.play(videoPath)
                            snapshotFlow { videoPlayerState.hasRenderedFirstFrame }
                                .first(true::equals)
                            videoPlayerController.pauseActiveVideo()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.alt_text_descriptive),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.weight(1f))
                // Circular progress or text for char count could be added here
                TextCircularProgress(
                    altText.codePointCount(),
                    AltTextCharacterLimit,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = altText,
                onValueChange = {
                    if (it.codePointCount() <= AltTextCharacterLimit) altText = it
                },
                placeholder = { Text(stringResource(Res.string.alt_text)) },
                minLines = 3,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = {
                    onMediaItemUpdated(media.withAltText(altText))
                    state.hide()
                },
            ) {
                Text(stringResource(CommonStrings.save))
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

private const val AltTextCharacterLimit = 2000
