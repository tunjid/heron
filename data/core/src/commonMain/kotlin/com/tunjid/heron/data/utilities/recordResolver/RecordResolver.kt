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
import app.bsky.graph.Listitem as BskyListMember
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.GetRecordResponse
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Block
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.FeedPreference
import com.tunjid.heron.data.core.models.Follow
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.Like
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Record.Reference
import com.tunjid.heron.data.core.models.Repost
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.AtProtoException
import com.tunjid.heron.data.core.types.BlockUri
import com.tunjid.heron.data.core.types.DerakkumaBestUri
import com.tunjid.heron.data.core.types.DerakkumaCircleMemberUri
import com.tunjid.heron.data.core.types.DerakkumaCircleUri
import com.tunjid.heron.data.core.types.DerakkumaFavoriteSongUri
import com.tunjid.heron.data.core.types.DerakkumaFriendUri
import com.tunjid.heron.data.core.types.DerakkumaPlayUri
import com.tunjid.heron.data.core.types.DerakkumaProfileUri
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionId
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.TrackUri
import com.tunjid.heron.data.core.types.UnknownRecordUri
import com.tunjid.heron.data.core.types.UnresolvableRecordException
import com.tunjid.heron.data.core.types.isNotFound
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.types.requireCollection
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StandardSiteDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.ThreadGateDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedLabelerEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedRecordEntity
import com.tunjid.heron.data.database.entities.PopulatedStandardDocumentEntity
import com.tunjid.heron.data.database.entities.PopulatedStandardPublicationEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.DefaultDispatcher
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.currentSessionContext
import com.tunjid.heron.data.network.models.asExternalModel
import com.tunjid.heron.data.network.models.post
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.distinctUntilChangedSignedProfilePreferencesOrDefault
import com.tunjid.heron.data.repository.expiredSessionResult
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.Collections.requireRecordUri
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.mapToResult
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver.TimelineItemCreationContext
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.tidInstant
import com.tunjid.heron.data.utilities.toDistinctUntilChangedFlowOrEmpty
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import site.standard.Document
import site.standard.graph.Subscription
import site.standard.heron.GetDocumentQueryParams
import site.standard.heron.GetPublicationQueryParams

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

    suspend fun resolveExternalLink(
        url: GenericUri,
    ): LinkPreview?

    suspend fun deleteRecord(
        uri: RecordUri,
    ): Outcome

    interface TimelineItemCreationContext {
        val top: TimelineItem?
        val post: Post
        val appliedLabels: AppliedLabels
        val signedInProfileId: ProfileId?
        val replyNodeIndex: MutableMap<PostUri, MutableList<TimelineItem.Threaded.Node>>

        infix fun push(item: TimelineItem)
        infix fun swapTop(item: TimelineItem)

        fun threadedNode(
            post: Post,
            depth: Int,
            children: List<TimelineItem.Threaded.Node> = emptyList(),
        ): TimelineItem.Threaded.Node

        fun record(recordUri: EmbeddableRecordUri): Record?
        fun profile(profileId: ProfileId): Profile?
        fun threadGate(postUri: PostUri): ThreadGate?
        fun isMuted(post: Post): Boolean
        fun feedPreference(source: Timeline.Source): FeedPreference
    }
}

@Inject
internal class OfflineRecordResolver(
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
    private val standardSiteDao: StandardSiteDao,
    private val starterPackDao: StarterPackDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val networkService: NetworkService,
    private val httpClient: HttpClient,
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
        val documentUris = LazyList<StandardDocumentUri>()
        val publicationUris = LazyList<StandardPublicationUri>()

        uris.forEach { uri ->
            when (uri) {
                is FeedGeneratorUri -> feedUris.add(uri)
                is ListUri -> listUris.add(uri)
                is PostUri -> postUris.add(uri)
                is StarterPackUri -> starterPackUris.add(uri)
                is LabelerUri -> labelerUris.add(uri)
                is StandardDocumentUri -> documentUris.add(uri)
                is StandardPublicationUri -> publicationUris.add(uri)
            }
        }

        val refreshChannel = Channel<RecordUri>(
            capacity = RefreshBufferSize,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        val queuedUris = mutableSetOf<RecordUri>()

        return combine(
            listOf(
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
                documentUris.list
                    .toDistinctUntilChangedFlowOrEmpty {
                        standardSiteDao.documents(viewingProfileId?.id, it)
                    },
                publicationUris.list
                    .toDistinctUntilChangedFlowOrEmpty {
                        standardSiteDao.publications(viewingProfileId?.id, it)
                    },
            ),
        ) { populatedRecordLists ->
            val associatedRecords = buildMap {
                populatedRecordLists.forEach { populatedRecords ->
                    populatedRecords.forEach { recordEntity ->
                        put(recordEntity.recordUri, recordEntity)
                        if (recordEntity.needsRefreshing() && queuedUris.add(recordEntity.recordUri)) {
                            refreshChannel.trySend(recordEntity.recordUri)
                        }
                    }
                }
            }
            associatedRecords.values.map { recordEntity ->
                when (recordEntity) {
                    // Top level posts have their embedded Records populated
                    is PopulatedPostEntity -> recordEntity.asExternalModel(
                        embeddedRecords = buildList {
                            recordEntity.entity
                                .record
                                ?.embeddedRecordUri
                                ?.let(::add)
                            recordEntity.associatedStandardRecords
                                .forEach { add(it.recordUri) }
                        }
                            .mapNotNull { uri ->
                                associatedRecords[uri]?.toEmbeddableModel()
                            }
                            .sortedBy(::embeddedRecordOrder),
                    )
                    else -> recordEntity.toEmbeddableModel()
                }
            }
        }
            .withRefresh {
                refreshChannel.consumeEach {
                    resolve(it)
                }
            }
            .onCompletion {
                refreshChannel.close()
            }
    }

    override suspend fun resolve(
        uri: RecordUri,
    ): Result<Record> = runCatchingUnlessCancelled {
        currentSessionContext()?.tokens
            ?.authProfileId
    }.mapToResult { viewingProfileId ->
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

            is ListMemberUri -> fetchRecordAndSaveCreator(
                recordUri = uri,
                viewingProfileId = viewingProfileId,
            ).mapToResult { recordResponse ->
                val bskyListMember = recordResponse.value.decodeAs<BskyListMember>()
                networkService.runCatchingWithMonitoredNetworkRetry {
                    getProfile(
                        GetProfileQueryParams(
                            actor = bskyListMember.subject,
                        ),
                    )
                }.mapCatchingUnlessCancelled { profileResponse ->
                    ListMember(
                        uri = uri,
                        subject = profileResponse.profileEntity().asExternalModel(),
                        listUri = bskyListMember.list.atUri.let(::ListUri),
                        createdAt = bskyListMember.createdAt,
                        viewerState = null,
                    )
                }
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

            is StandardPublicationUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getPublication(
                    GetPublicationQueryParams(
                        uri = uri.uri.let(::AtUri),
                    ),
                )
            }
                .mapCatchingUnlessCancelled { response ->
                    multipleEntitySaverProvider.saveInTransaction {
                        add(
                            publicationView = response.publication,
                            viewingProfileId = viewingProfileId,
                        )
                    }

                    standardSiteDao.publication(
                        viewingProfileId = viewingProfileId?.id,
                        publicationUri = uri.uri,
                    )
                        .first()
                        .asExternalModel()
                }

            is StandardDocumentUri -> networkService.runCatchingWithMonitoredNetworkRetry(times = 2) {
                getDocument(
                    GetDocumentQueryParams(
                        uri = uri.uri.let(::AtUri),
                    ),
                )
            }
                .mapCatchingUnlessCancelled { response ->
                    val documentView = response.document
                    val document = response.document.record.decodeAs<Document>()

                    multipleEntitySaverProvider.saveInTransaction {
                        add(
                            documentView = response.document,
                            viewingProfileId = viewingProfileId,
                        )
                    }

                    StandardDocument(
                        uri = uri,
                        cid = documentView.cid.cid.let(::StandardDocumentId),
                        authorId = documentView.author.did.did.let(::ProfileId),
                        title = document.title,
                        description = document.description,
                        textContent = document.textContent,
                        path = document.path,
                        site = document.site.uri,
                        publishedAt = document.publishedAt,
                        updatedAt = document.updatedAt,
                        coverImage = documentView.coverImageUrl?.uri?.let(::ImageUri),
                        bskyPostRef = document.bskyPostRef?.let { ref ->
                            Reference(
                                id = ref.cid.cid.let(::PostId),
                                uri = PostUri(ref.uri.atUri),
                            )
                        },
                        tags = document.tags ?: emptyList(),
                        publication = documentView.publication
                            ?.uri
                            ?.atUri
                            ?.let {
                                standardSiteDao.publication(
                                    viewingProfileId = viewingProfileId?.id,
                                    publicationUri = it,
                                )
                            }
                            ?.firstOrNull()
                            ?.asExternalModel(),
                    )
                }

            is StandardSubscriptionUri -> if (viewingProfileId != null) {
                fetchRecordAndSaveCreator(uri, viewingProfileId)
                    .mapCatchingUnlessCancelled { response ->
                        val subscription = response.value.decodeAs<Subscription>()
                        val cid = response.cid?.cid?.let(::StandardSubscriptionId)
                        val sortedAt = uri.recordKey.tidInstant ?: Instant.DISTANT_PAST
                        multipleEntitySaverProvider.saveInTransaction {
                            add(
                                subscriptionUri = uri,
                                subscriptionCid = cid,
                                subscription = subscription,
                                sortedAt = sortedAt,
                                viewingProfileId = viewingProfileId,
                            )
                        }
                        StandardSubscription(
                            uri = uri,
                            cid = cid,
                            sortedAt = sortedAt,
                            publicationUri = StandardPublicationUri(
                                subscription.publication.atUri,
                            ),
                        )
                    }
            } else {
                expiredSessionResult()
            }
            is AlbumUri -> Result.failure(UnresolvableRecordException(uri)) // TODO
            is ArtistUri -> Result.failure(UnresolvableRecordException(uri)) // TODO
            is DerakkumaProfileUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaPlayUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaBestUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaFriendUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaFavoriteSongUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaCircleUri -> Result.failure(UnresolvableRecordException(uri))
            is DerakkumaCircleMemberUri -> Result.failure(UnresolvableRecordException(uri))
            is ScrobbleUri -> Result.failure(UnresolvableRecordException(uri)) // TODO
            is TrackUri -> Result.failure(UnresolvableRecordException(uri)) // TODO
            is UnknownRecordUri -> Result.failure(UnresolvableRecordException(uri))
        }
    }.onFailure { throwable ->
        logcat(LogPriority.WARN) {
            "Failed to resolve $uri. Cause: ${throwable.loggableText()}"
        }
        if (throwable.isNotFound()) {
            deleteLocalRecord(uri)
        }
    }

    override suspend fun resolveExternalLink(
        url: GenericUri,
    ): LinkPreview? = httpClient.linkPreviewOrNull(url)

    override suspend fun deleteRecord(
        uri: RecordUri,
    ) = runCatchingUnlessCancelled {
        networkService.runCatchingWithMonitoredNetworkRetry {
            deleteRecord(
                DeleteRecordRequest(
                    repo = Did(uri.profileId().id),
                    collection = Nsid(uri.requireCollection()),
                    rkey = RKey(uri.recordKey.value),
                ),
            )
        }.getOrThrow()

        deleteLocalRecord(uri)
    }.toOutcome()

    private suspend fun deleteLocalRecord(
        uri: RecordUri,
    ) {
        when (uri) {
            is BlockUri -> profileDao.deleteBlock(uri)
            is FeedGeneratorUri -> feedGeneratorDao.deleteFeedGenerator(uri)
            is LabelerUri -> labelDao.deleteLabeler(uri)
            is ListUri -> listDao.deleteList(uri)
            is ListMemberUri -> listDao.deleteListMember(uri)
            is PostUri -> postDao.deletePost(uri)
            is StarterPackUri -> starterPackDao.deleteStarterPack(uri)
            is FollowUri -> profileDao.deleteFollow(uri)
            is LikeUri -> postDao.deletePostViewerStatisticsLike(uri)
            is RepostUri -> postDao.deletePostViewerStatisticsRepost(uri)
            is StandardPublicationUri -> standardSiteDao.deletePublication(uri)
            is StandardDocumentUri -> standardSiteDao.deleteDocument(uri)
            is StandardSubscriptionUri -> standardSiteDao.deleteSubscription(uri)
            is AlbumUri -> Unit // TODO
            is ArtistUri -> Unit // TODO
            is DerakkumaProfileUri -> Unit
            is DerakkumaPlayUri -> Unit
            is DerakkumaBestUri -> Unit
            is DerakkumaFriendUri -> Unit
            is DerakkumaFavoriteSongUri -> Unit
            is DerakkumaCircleUri -> Unit
            is DerakkumaCircleMemberUri -> Unit
            is ScrobbleUri -> Unit // TODO
            is TrackUri -> Unit // TODO
            is UnknownRecordUri -> Unit
        }
    }

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

/**
 * Maps a resolved [PopulatedRecordEntity] to its [Record.Embeddable] model. Nested posts
 * (eg. a quote of a quote) are resolved one level deep with empty [Post.embeddedRecords].
 */
private fun PopulatedRecordEntity.toEmbeddableModel(): Record.Embeddable = when (this) {
    is PopulatedFeedGeneratorEntity -> asExternalModel()
    is PopulatedLabelerEntity -> asExternalModel()
    is PopulatedListEntity -> asExternalModel()
    is PopulatedStandardDocumentEntity -> asExternalModel()
    is PopulatedStandardPublicationEntity -> asExternalModel()
    is PopulatedStarterPackEntity -> asExternalModel()
    is PopulatedPostEntity -> asExternalModel(embeddedRecords = emptyList())
}

private fun PopulatedRecordEntity.needsRefreshing() =
    when (this) {
        is PopulatedFeedGeneratorEntity,
        is PopulatedLabelerEntity,
        is PopulatedListEntity,
        is PopulatedPostEntity,
        is PopulatedStarterPackEntity,
        is PopulatedStandardDocumentEntity,
        -> false
        is PopulatedStandardPublicationEntity,
        -> this.entity.cid == null && entity.url == Collections.PLACEHOLDER_URL
    }

/**
 * Stable ordering for a [Post]'s embedded records: (1) quoted post, (2) other bluesky embedded
 * records (feed generator/list/starter pack/labeler), (3) standard-site records.
 */
private fun embeddedRecordOrder(record: Record.Embeddable): Int = when (record) {
    is Post -> 0
    is FeedGenerator,
    is FeedList,
    is StarterPack,
    is Labeler,
    -> 1
    is StandardDocument,
    -> 2
    is StandardPublication,
    -> 3
}

private const val RefreshBufferSize = 64
