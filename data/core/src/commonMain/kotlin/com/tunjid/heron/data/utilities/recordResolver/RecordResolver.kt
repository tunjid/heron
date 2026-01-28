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

import app.bsky.actor.GetProfileQueryParams
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.GetPostsQueryParams
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Repost as BskyRepost
import app.bsky.graph.Block as BskyBlock
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetStarterPackQueryParams
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.GetRecordResponse
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Block
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.Follow
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.Like
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Repost
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.types.BlockUri
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.UnknownRecordUri
import com.tunjid.heron.data.core.types.UnresolvableRecordException
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.types.requireCollection
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.ThreadGateDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedLabelerEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.DefaultDispatcher
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.asExternalModel
import com.tunjid.heron.data.network.models.post
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.distinctUntilChangedSignedProfilePreferencesOrDefault
import com.tunjid.heron.data.repository.expiredSessionResult
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.Collections.requireRecordUri
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver.TimelineItemCreationContext
import com.tunjid.heron.data.utilities.toDistinctUntilChangedFlowOrEmpty
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

internal interface RecordResolver {
    val subscribedLabelers: Flow<List<Labeler>>

    fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
        viewingProfileId: ProfileId?,
    ): Flow<List<Record.Embeddable>>

    fun <T> timelineItems(
        items: List<T>,
        signedInProfileId: ProfileId?,
        postUri: (T) -> PostUri,
        associatedRecordUris: (T) -> List<EmbeddableRecordUri>,
        associatedProfileIds: (T) -> List<ProfileId>,
        block: TimelineItemCreationContext.(T) -> Unit,
    ): Flow<List<TimelineItem>>

    suspend fun resolve(
        uri: RecordUri,
    ): Result<Record>

    interface TimelineItemCreationContext {
        val list: MutableList<TimelineItem>
        val post: Post
        val appliedLabels: AppliedLabels
        val signedInProfileId: ProfileId?

        fun record(recordUri: EmbeddableRecordUri): Record?
        fun profile(profileId: ProfileId): Profile?
        fun threadGate(postUri: PostUri): ThreadGate?
        fun isMuted(post: Post): Boolean
    }
}

internal class OfflineRecordResolver @Inject constructor(
    @AppMainScope
    appMainScope: CoroutineScope,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val labelDao: LabelDao,
    private val listDao: ListDao,
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val threadGateDao: ThreadGateDao,
    private val starterPackDao: StarterPackDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val networkService: NetworkService,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
) : RecordResolver {

    override val subscribedLabelers: Flow<List<Labeler>> =
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
                scope = appMainScope + ioDispatcher,
                started = SharingStarted.WhileSubscribed(1_000),
                initialValue = emptyList(),
            )

    override fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
        viewingProfileId: ProfileId?,
    ): Flow<List<Record.Embeddable>> {
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
                .toDistinctUntilChangedFlowOrEmpty(feedGeneratorDao::feedGenerators),
            listUris.list
                .toDistinctUntilChangedFlowOrEmpty(listDao::lists),
            postUris.list
                .toDistinctUntilChangedFlowOrEmpty { postDao.posts(viewingProfileId?.id, it) },
            starterPackUris.list
                .toDistinctUntilChangedFlowOrEmpty(starterPackDao::starterPacks),
            labelerUris.list
                .toDistinctUntilChangedFlowOrEmpty(labelDao::labelers),
        ) { feeds, lists, posts, starterPacks, labelers ->
            val associatedRecords = buildMap {
                feeds.forEach { put(it.recordUri, it) }
                lists.forEach { put(it.recordUri, it) }
                posts.forEach { put(it.recordUri, it) }
                starterPacks.forEach { put(it.recordUri, it) }
                labelers.forEach { put(it.recordUri, it) }
            }
            associatedRecords.values.map { recordEntity ->
                when (recordEntity) {
                    is PopulatedFeedGeneratorEntity -> recordEntity.asExternalModel()
                    is PopulatedLabelerEntity -> recordEntity.asExternalModel()
                    is PopulatedListEntity -> recordEntity.asExternalModel()
                    is PopulatedPostEntity -> recordEntity.asExternalModel(
                        embeddedRecord = when (
                            val embeddedRecordEntity =
                                associatedRecords[recordEntity.entity.record?.embeddedRecordUri]
                        ) {
                            is PopulatedFeedGeneratorEntity -> embeddedRecordEntity.asExternalModel()
                            is PopulatedLabelerEntity -> embeddedRecordEntity.asExternalModel()
                            is PopulatedListEntity -> embeddedRecordEntity.asExternalModel()
                            is PopulatedPostEntity -> embeddedRecordEntity.asExternalModel(
                                embeddedRecord = null,
                            )
                            is PopulatedStarterPackEntity -> embeddedRecordEntity.asExternalModel()
                            null -> null
                        },
                    )
                    is PopulatedStarterPackEntity -> recordEntity.asExternalModel()
                }
            }
        }
    }

    override suspend fun resolve(
        uri: RecordUri,
    ): Result<Record> = savedStateDataSource.inCurrentProfileSession { viewingProfileId ->
        when (uri) {
            is FeedGeneratorUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getFeedGenerator(
                    GetFeedGeneratorQueryParams(
                        feed = uri.uri.let(::AtUri),
                    ),
                )
            }
                .mapCatchingUnlessCancelled {
                    multipleEntitySaverProvider.saveInTransaction { add(it.view) }
                    it.view.asExternalModel()
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
                .mapCatchingUnlessCancelled {
                    multipleEntitySaverProvider.saveInTransaction { add(it.list) }
                    it.list.asExternalModel()
                }

            is PostUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getPosts(
                    GetPostsQueryParams(
                        uris = listOf(uri.uri.let(::AtUri)),
                    ),
                )
            }
                .mapCatchingUnlessCancelled {
                    val postView = it.posts.first()
                    multipleEntitySaverProvider.saveInTransaction {
                        add(
                            viewingProfileId = viewingProfileId,
                            postView = postView,
                        )
                    }
                    postView.post(viewingProfileId)
                }

            is StarterPackUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getStarterPack(
                    GetStarterPackQueryParams(
                        starterPack = uri.uri.let(::AtUri),
                    ),
                )
            }
                .mapCatchingUnlessCancelled {
                    multipleEntitySaverProvider.saveInTransaction { add(it.starterPack) }
                    it.starterPack.asExternalModel()
                }
            is LabelerUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getServices(
                    GetServicesQueryParams(
                        dids = listOf(uri.profileId().id.let(::Did)),
                        detailed = true,
                    ),
                )
            }
                .mapCatchingUnlessCancelled {
                    val responseViewUnion = it.views.first()
                    multipleEntitySaverProvider.saveInTransaction {
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
                    when (responseViewUnion) {
                        is GetServicesResponseViewUnion.LabelerView -> responseViewUnion.value.asExternalModel()
                        is GetServicesResponseViewUnion.LabelerViewDetailed -> responseViewUnion.value.asExternalModel()
                        is GetServicesResponseViewUnion.Unknown -> throw UnresolvableRecordException(
                            uri,
                        )
                    }
                }
            is FollowUri -> fetchRecordAndSaveCreator(uri, viewingProfileId)
                .mapCatchingUnlessCancelled {
                    Follow(
                        uri = uri,
                        cid = GenericId(requireNotNull(it.cid).cid),
                    )
                }

            is LikeUri -> fetchRecordAndSaveCreator(uri, viewingProfileId)
                .mapCatchingUnlessCancelled {
                    val bskyLike = it.value.decodeAs<BskyLike>()
                    val subjectUri = bskyLike.subject.uri.requireRecordUri()
                    require(subjectUri is PostUri)
                    val resolvedRecord = requireNotNull(resolve(subjectUri).getOrNull())
                    require(resolvedRecord is Post)

                    Like(
                        uri = uri,
                        cid = GenericId(requireNotNull(it.cid).cid),
                        post = resolvedRecord,
                        via = bskyLike.via?.uri?.requireRecordUri(),
                    )
                }

            is RepostUri -> fetchRecordAndSaveCreator(uri, viewingProfileId)
                .mapCatchingUnlessCancelled {
                    val bskyRepost = it.value.decodeAs<BskyRepost>()
                    val subjectUri = bskyRepost.subject.uri.requireRecordUri()
                    require(subjectUri is PostUri)
                    val resolvedRecord = requireNotNull(resolve(subjectUri).getOrNull())
                    require(resolvedRecord is Post)

                    Repost(
                        uri = uri,
                        cid = GenericId(requireNotNull(it.cid).cid),
                        post = resolvedRecord,
                        via = bskyRepost.via?.uri?.requireRecordUri(),
                    )
                }
            is BlockUri -> fetchRecordAndSaveCreator(uri, viewingProfileId)
                .mapCatchingUnlessCancelled {
                    val bskyBlock = it.value.decodeAs<BskyBlock>()
                    Block(
                        uri = uri,
                        cid = GenericId(requireNotNull(it.cid).cid),
                        subject = bskyBlock.subject.did.let(::ProfileId),
                    )
                }
            is UnknownRecordUri -> Result.failure(UnresolvableRecordException(uri))
        }
    }?.onFailure {
        logcat(LogPriority.WARN) {
            "Failed to resolve $uri. Cause: ${it.loggableText()}"
        }
    } ?: expiredSessionResult()

    override fun <T> timelineItems(
        items: List<T>,
        signedInProfileId: ProfileId?,
        postUri: (T) -> PostUri,
        associatedRecordUris: (T) -> List<EmbeddableRecordUri>,
        associatedProfileIds: (T) -> List<ProfileId>,
        block: TimelineItemCreationContext.(T) -> Unit,
    ): Flow<List<TimelineItem>> =
        savedStateDataSource.distinctUntilChangedSignedProfilePreferencesOrDefault()
            .flatMapLatest { preferences ->
                val allowAdultContent = preferences.allowAdultContent
                val labelsVisibilityMap = preferences.contentLabelPreferences.associateBy(
                    keySelector = ContentLabelPreference::label,
                    valueTransform = ContentLabelPreference::visibility,
                )

                val recordUris = mutableSetOf<EmbeddableRecordUri>()
                val threadGatePostUris = mutableListOf<PostUri>()
                val profileIds = mutableSetOf<ProfileId>()

                items.forEach { item ->
                    postUri(item).let { itemPostUri ->
                        recordUris.add(itemPostUri)
                        threadGatePostUris.add(itemPostUri)
                    }
                    associatedRecordUris(item).forEach { associatedRecordUri ->
                        recordUris.add(associatedRecordUri)
                        if (associatedRecordUri is PostUri) {
                            threadGatePostUris.add(associatedRecordUri)
                        }
                    }
                    profileIds.addAll(associatedProfileIds(item))
                }

                combine(
                    flow = embeddableRecords(
                        uris = recordUris,
                        viewingProfileId = signedInProfileId,
                    )
                        .distinctUntilChanged(),
                    flow2 = threadGatePostUris
                        .toDistinctUntilChangedFlowOrEmpty(threadGateDao::threadGates),
                    flow3 = profileIds
                        .toDistinctUntilChangedFlowOrEmpty {
                            profileDao.profiles(
                                signedInProfiledId = signedInProfileId?.id,
                                ids = it,
                            )
                        },
                    flow4 = subscribedLabelers,
                    transform = { associatedRecords, threadGateEntities, profileEntities, labelers ->
                        if (associatedRecords.isEmpty()) return@combine emptyList()
                        withContext(defaultDispatcher) {
                            items.fold(
                                MutableTimelineItemCreationContext(
                                    signedInProfileId = signedInProfileId,
                                    preferences = preferences,
                                    associatedRecords = associatedRecords,
                                    associatedThreadGateEntities = threadGateEntities,
                                    associatedProfileEntities = profileEntities,
                                ),
                            ) { context, item ->
                                val post = context.record(postUri(item)) as? Post
                                    ?: return@fold context

                                // Always omit blocked users
                                if (post.viewerState.isBlocked) return@fold context

                                val postLabels = when {
                                    post.labels.isEmpty() -> emptySet()
                                    else -> post.labels.mapTo(
                                        destination = mutableSetOf(),
                                        transform = Label::value,
                                    )
                                }

                                // Check for global hidden label
                                if (postLabels.contains(Label.Hidden)) return@fold context

                                // Check for global non authenticated label
                                val isSignedIn = signedInProfileId != null
                                if (!isSignedIn && postLabels.contains(Label.NonAuthenticated)) return@fold context

                                val appliedLabels = AppliedLabels(
                                    adultContentEnabled = allowAdultContent,
                                    labels = post.labels + post.author.labels,
                                    labelers = labelers,
                                    preferenceLabelsVisibilityMap = labelsVisibilityMap,
                                )

                                if (appliedLabels.shouldHide) return@fold context

                                context.apply {
                                    update(
                                        currentPost = post,
                                        appliedLabels = appliedLabels,
                                    )
                                    block(item)
                                }
                            }
                        }
                    },
                ).distinctUntilChanged()
            }

    private suspend fun fetchRecordAndSaveCreator(
        recordUri: RecordUri,
        viewingProfileId: ProfileId?,
    ): Result<GetRecordResponse> = coroutineScope {
        // Get the profile independently
        launch {
            networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getProfile(
                    GetProfileQueryParams(actor = recordUri.profileId().id.let(::Did)),
                )
            }
                .mapCatchingUnlessCancelled { profileViewDetailed ->
                    multipleEntitySaverProvider.saveInTransaction {
                        add(
                            viewingProfileId = viewingProfileId,
                            profileView = profileViewDetailed,
                        )
                    }
                }
        }
        networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
            getRecord(
                GetRecordQueryParams(
                    repo = recordUri.profileId().id.let(::Did),
                    collection = recordUri.requireCollection().let(::Nsid),
                    rkey = recordUri.recordKey.value.let(::RKey),
                ),
            )
        }
    }
}
