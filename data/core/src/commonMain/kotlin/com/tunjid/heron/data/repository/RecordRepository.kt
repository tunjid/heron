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

import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.GetPostsQueryParams
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetStarterPackQueryParams
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import sh.christian.ozone.api.AtUri

interface RecordRepository {

    fun record(uri: RecordUri): Flow<Record>
}

internal class OfflineRecordRepository @Inject constructor(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : RecordRepository {

    override fun record(uri: RecordUri): Flow<Record> =
        when (uri) {
            is FeedGeneratorUri -> feedGeneratorDao.feedGenerators(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is ListUri -> listDao.lists(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is StarterPackUri -> starterPackDao.starterPacks(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is PostUri ->
                savedStateDataSource
                    .observedSignedInProfileId
                    .flatMapLatest { profileId ->
                        postDao.posts(
                            viewingProfileId = profileId?.id,
                            postUris = listOf(uri),
                        )
                    }
                    .map { it.firstOrNull()?.asExternalModel(quote = null) }
        }
            .filterNotNull()
            .withRefresh {
                networkService.refresh(
                    uri = uri,
                    savedStateDataSource = savedStateDataSource,
                    multipleEntitySaverProvider = multipleEntitySaverProvider,
                )
            }
}

internal suspend fun NetworkService.refresh(
    uri: RecordUri,
    savedStateDataSource: SavedStateDataSource,
    multipleEntitySaverProvider: MultipleEntitySaverProvider,
) = savedStateDataSource.inCurrentProfileSession { viewingProfileId ->
    when (uri) {
        is FeedGeneratorUri -> runCatchingWithMonitoredNetworkRetry(times = 2) {
            getFeedGenerator(
                GetFeedGeneratorQueryParams(
                    feed = uri.uri.let(::AtUri),
                ),
            )
        }
            .getOrNull()
            ?.view
            ?.let { feedGeneratorView ->
                multipleEntitySaverProvider.saveInTransaction { add(feedGeneratorView) }
            }

        is ListUri -> runCatchingWithMonitoredNetworkRetry(times = 2) {
            getList(
                GetListQueryParams(
                    cursor = null,
                    limit = 1,
                    list = uri.uri.let(::AtUri),
                ),
            )
        }
            .getOrNull()
            ?.list
            ?.let {
                multipleEntitySaverProvider.saveInTransaction { add(it) }
            }

        is PostUri -> runCatchingWithMonitoredNetworkRetry(times = 2) {
            getPosts(
                GetPostsQueryParams(
                    uris = listOf(uri.uri.let(::AtUri)),
                ),
            )
        }
            .getOrNull()
            ?.posts
            ?.let { postViews ->
                multipleEntitySaverProvider.saveInTransaction {
                    postViews.forEach { postView ->
                        add(
                            viewingProfileId = viewingProfileId,
                            postView = postView,
                        )
                    }
                }
            }

        is StarterPackUri -> runCatchingWithMonitoredNetworkRetry(times = 2) {
            getStarterPack(
                GetStarterPackQueryParams(
                    starterPack = uri.uri.let(::AtUri),
                ),
            )
        }
            .getOrNull()
            ?.starterPack
            ?.let { starterPackView ->
                multipleEntitySaverProvider.saveInTransaction { add(starterPackView) }
            }
    }
}.let { }
