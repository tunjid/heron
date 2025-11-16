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

package com.tunjid.heron.data.utilities.recordResolver

import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.GetPostsQueryParams
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetStarterPackQueryParams
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LabelerPreference
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
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.toFlowOrEmpty
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

interface RecordResolver {
    val labelers: Flow<List<Labeler>>

    fun records(
        uris: Set<RecordUri>,
        viewingProfileId: ProfileId?,
    ): Flow<List<Record>>

    suspend fun refresh(
        uri: RecordUri,
    )
}

internal class OfflineRecordResolver @Inject constructor(
    @Named("AppScope") appScope: CoroutineScope,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val labelDao: LabelDao,
    private val listDao: ListDao,
    private val postDao: PostDao,
    private val starterPackDao: StarterPackDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val networkService: NetworkService,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
) : RecordResolver {

    override val labelers: Flow<List<Labeler>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            savedStateDataSource.savedState.map {
                it.signedInProfileData
                    ?.preferences
                    ?.labelerPreferences
                    ?.map(LabelerPreference::labelerCreatorId)
                    ?.plus(Collections.DefaultLabelerProfileId)
                    ?: listOf(Collections.DefaultLabelerProfileId)
            }
                .distinctUntilChanged()
                .flatMapLatest { labelerCreatorIds ->
                    labelDao.labelersByCreators(labelerCreatorIds)
                        .map { it.map(PopulatedLabelerEntity::asExternalModel) }
                        .withRefresh {
                            networkService.runCatchingWithMonitoredNetworkRetry {
                                getServices(
                                    GetServicesQueryParams(
                                        dids = labelerCreatorIds.map { Did(it.id) },
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
                                                    viewingProfileId = signedInProfileId,
                                                    labeler = responseViewUnion.value,
                                                )
                                                is GetServicesResponseViewUnion.LabelerViewDetailed -> add(
                                                    viewingProfileId = signedInProfileId,
                                                    labeler = responseViewUnion.value,
                                                )
                                                is GetServicesResponseViewUnion.Unknown -> Unit
                                            }
                                        }
                                    }
                                }
                        }
                }
        }
            .stateIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(1_000),
                initialValue = emptyList(),
            )

    override fun records(
        uris: Set<RecordUri>,
        viewingProfileId: ProfileId?,
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

    override suspend fun refresh(
        uri: RecordUri,
    ) = savedStateDataSource.inCurrentProfileSession { viewingProfileId ->
        when (uri) {
            is FeedGeneratorUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
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

            is ListUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
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

            is PostUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
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

            is StarterPackUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
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
            is LabelerUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
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
}
