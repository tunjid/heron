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

package com.tunjid.heron.timeline.ui.standard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.DocumentCollectionShape
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.RecordBlurb
import com.tunjid.heron.ui.RecordSubtitle
import com.tunjid.heron.ui.RecordText
import com.tunjid.heron.ui.RecordTitle
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.standard_site_published_in
import org.jetbrains.compose.resources.stringResource

@Composable
fun Document(
    modifier: Modifier = Modifier,
    document: StandardDocument,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth(),
            avatar = {
                document.coverImage?.let { cover ->
                    AsyncImage(
                        modifier = Modifier
                            .size(44.dp),
                        args = remember(
                            cover,
                        ) {
                            ImageArgs(
                                url = cover.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                shape = DocumentCollectionShape,
                            )
                        },
                    )
                }
            },
            label = {
                RecordTitle(
                    title = document.title,
                )
                Spacer(Modifier.height(8.dp))
                document.publication?.let { publication ->
                    val publisherDescription = stringResource(
                        Res.string.standard_site_published_in,
                        publication.name,
                    )
                    val uriHandler = LocalUriHandler.current
                    Label(
                        contentDescription = publisherDescription,
                        icon = {
                            publication.icon?.let { icon ->
                                AsyncImage(
                                    modifier = Modifier
                                        .size(16.dp),
                                    args = remember(
                                        icon,
                                    ) {
                                        ImageArgs(
                                            url = icon.uri,
                                            contentScale = ContentScale.Crop,
                                            contentDescription = null,
                                            shape = DocumentCollectionShape,
                                        )
                                    },
                                )
                            }
                        },
                        description = {
                            RecordSubtitle(
                                subtitle = publisherDescription,
                            )
                        },
                        onClick = {
                            runCatching {
                                uriHandler.openUri(publication.url)
                            }
                        },
                    )
                }
            },
        )

        document.description.takeUnless(String?::isNullOrEmpty)?.let {
            RecordText(
                text = it,
            )
        }
        LabelFlowRow {
            document.tags.forEach {
                Label(
                    contentDescription = it,
                    isElevated = true,
                    icon = {},
                    description = {
                        RecordBlurb(it)
                    },
                    onClick = {},
                )
            }
        }
    }
}
