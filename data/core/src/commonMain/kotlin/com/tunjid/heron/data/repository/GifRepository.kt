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

package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.core.models.canRequestData
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import social.heron.gif.GetTrendingGifsQueryParams
import social.heron.gif.GifView
import social.heron.gif.MediaFormat
import social.heron.gif.SearchGifsQueryParams

@Serializable
sealed class GifQuery : CursorQuery {

    @Serializable
    data class Trending(
        override val data: CursorQuery.Data,
    ) : GifQuery()

    @Serializable
    data class Search(
        val query: String,
        override val data: CursorQuery.Data,
    ) : GifQuery()
}

interface GifRepository {
    fun gifs(
        query: GifQuery,
        cursor: Cursor,
    ): Flow<CursorList<Gif>>
}

@Inject
internal class NetworkGifRepository(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : GifRepository {

    override fun gifs(
        query: GifQuery,
        cursor: Cursor,
    ): Flow<CursorList<Gif>> =
        if (!cursor.canRequestData) emptyFlow()
        else if (query is GifQuery.Search && query.query.isBlank()) emptyFlow()
        else savedStateDataSource.singleAuthorizedSessionFlow {
            flow {
                val page = when (query) {
                    is GifQuery.Search -> networkService.runCatchingWithMonitoredNetworkRetry {
                        searchGifs(
                            params = SearchGifsQueryParams(
                                q = query.query,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    }.map { response ->
                        response.gifs.asCursorList(
                            cursor = response.cursor,
                        )
                    }
                    is GifQuery.Trending -> networkService.runCatchingWithMonitoredNetworkRetry {
                        getTrendingGifs(
                            params = GetTrendingGifsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    }.map { response ->
                        response.gifs.asCursorList(
                            cursor = response.cursor,
                        )
                    }
                }
                    .getOrNull()
                    ?: return@flow

                emit(page)
            }
        }
            .filterNotNull()
            .flowOn(ioDispatcher)
}

private fun List<GifView>.asCursorList(
    cursor: String?,
): CursorList<Gif> = CursorList(
    items = map(GifView::asExternalModel),
    nextCursor = cursor?.let(Cursor::Next) ?: Cursor.Final,
)

private fun GifView.asExternalModel() = Gif(
    id = id,
    title = title,
    description = description,
    gif = gif.asExternalModel(),
    preview = preview.asExternalModel(),
    tinyGif = tinyGif?.asExternalModel(),
    mp4 = mp4?.asExternalModel(),
    webm = webm?.asExternalModel(),
)

private fun MediaFormat.asExternalModel() = Gif.Media(
    uri = GenericUri(url.uri),
    width = width,
    height = height,
)
