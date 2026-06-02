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

package com.tunjid.heron.timeline.ui.rocksky

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.RockskyAlbum
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.timeline.utilities.LabelText
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.album_by
import heron.ui.timeline.generated.resources.played_n_times
import kotlinx.datetime.LocalDate
import kotlinx.datetime.parseOrNull
import org.jetbrains.compose.resources.stringResource

@Composable
fun RockskyAlbum(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    album: RockskyAlbum,
    onMusicServiceLinkClicked: (String) -> Unit,
) =
    with(paneTransitionScope) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordLayout(
                modifier = Modifier.fillMaxWidth(),
                paneTransitionScope = paneTransitionScope,
                title = album.title,
                subtitle =
                    stringResource(
                        Res.string.album_by,
                        album.artist,
                    ),
                description =
                    dotSeparatedText(
                        preText =
                            remember(album.releaseDate) {
                                album.releaseDate?.let(LocalDate::parseOrNull)?.year?.toString()
                            } ?: album.year?.toString(),
                        postText =
                            album.playCount?.let { count ->
                                stringResource(
                                    Res.string.played_n_times,
                                    format(count),
                                )
                            },
                    ),
                blurb = null,
                sharedElementPrefix = sharedElementPrefix,
                sharedElementType = album.uri,
                avatar = {
                    RockSkyAvatar(
                        image = album.albumArt,
                        uri = album.uri,
                        sharedElementPrefix = sharedElementPrefix,
                    )
                },
            )
            if (album.hasMusicServiceLink) {
                LabelFlowRow(modifier = Modifier.align(Alignment.End)) {
                    album.forEachMusicServiceLink { labelRes, iconUri, musicServiceUrl ->
                        val description = stringResource(labelRes)
                        Label(
                            contentDescription = description,
                            isElevated = true,
                            icon = {
                                MusicServiceIcon(iconUri = iconUri)
                            },
                            description = {
                                LabelText(text = description)
                            },
                            onClick = {
                                if (musicServiceUrl.startsWith(Uri.Host.Https.prefix)) {
                                    onMusicServiceLinkClicked(musicServiceUrl)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
