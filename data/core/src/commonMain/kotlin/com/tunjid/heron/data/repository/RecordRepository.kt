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
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedLabelerEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.toFlowOrEmpty
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

interface RecordRepository {

    fun record(uri: RecordUri): Flow<Record>
}

internal class OfflineRecordRepository @Inject constructor(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val labelDao: LabelDao,
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
            is LabelerUri -> labelDao.labelers(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is PostUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        postDao.posts(
                            viewingProfileId = profileId?.id,
                            postUris = listOf(uri),
                        )
                    }
                    .map {
                        it.firstOrNull()?.asExternalModel(
                            embeddedRecord = null,
                        )
                    }
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

internal fun records(
    uris: Set<RecordUri>,
    viewingProfileId: ProfileId?,
    feedGeneratorDao: FeedGeneratorDao,
    labelDao: LabelDao,
    listDao: ListDao,
    postDao: PostDao,
    starterPackDao: StarterPackDao,
): Flow<List<Record>> {
    val feedUris = LazyList<FeedGeneratorUri>()
    val listUris = LazyList<ListUri>()
    val postUris = LazyList<PostUri>()
    val starterPackUris = LazyList<StarterPackUri>()
    val labelerUris = LazyList<LabelerUri>()

    uris.forEach { uri ->
        when (uri) {
            is FeedGeneratorUri -> feedUris.add(uri)
            is ListUri -> listUris.add(uri)
            is PostUri -> postUris.add(uri)
            is StarterPackUri -> starterPackUris.add(uri)
            is LabelerUri -> labelerUris.add(uri)
        }
    }

    return combine(
        feedUris.list
            .toFlowOrEmpty(feedGeneratorDao::feedGenerators)
            .distinctUntilChanged()
            .map { entities ->
                entities.map(PopulatedFeedGeneratorEntity::asExternalModel)
            },
        listUris.list
            .toFlowOrEmpty(listDao::lists)
            .distinctUntilChanged()
            .map { entities ->
                entities.map(PopulatedListEntity::asExternalModel)
            },
        postUris.list
            .toFlowOrEmpty { postDao.posts(viewingProfileId?.id, it) }
            .distinctUntilChanged()
            .map { entities ->
                entities.map {
                    it.asExternalModel(
                        embeddedRecord = null,
                    )
                }
            },
        starterPackUris.list
            .toFlowOrEmpty(starterPackDao::starterPacks)
            .distinctUntilChanged()
            .map { entities ->
                entities.map(PopulatedStarterPackEntity::asExternalModel)
            },
        labelerUris.list
            .toFlowOrEmpty(labelDao::labelers)
            .distinctUntilChanged()
            .map { entities ->
                entities.map(PopulatedLabelerEntity::asExternalModel)
            },
    ) { feeds, lists, posts, starterPacks, labelers ->
        feeds + lists + posts + starterPacks + labelers
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
        is LabelerUri -> runCatchingWithMonitoredNetworkRetry(times = 2) {
            getServices(
                GetServicesQueryParams(
                    dids = listOf(uri.profileId().id.let(::Did)),
                    detailed = true,
                ),
            )
        }
            .getOrNull()
            ?.views
            ?.let { responseViewUnionList ->
                multipleEntitySaverProvider.saveInTransaction {
                    responseViewUnionList.forEach { responseViewUnion ->
                        when (responseViewUnion) {
                            is GetServicesResponseViewUnion.LabelerView -> add(
                                viewingProfileId = viewingProfileId,
                                labeler = responseViewUnion.value,
                            )
                            is GetServicesResponseViewUnion.LabelerViewDetailed -> add(
                                viewingProfileId = viewingProfileId,
                                labeler = responseViewUnion.value,
                            )
                            is GetServicesResponseViewUnion.Unknown -> Unit
                        }
                    }
                }
            }
    }
}.let { }
