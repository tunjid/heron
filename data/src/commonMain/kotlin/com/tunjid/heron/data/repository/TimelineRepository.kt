package com.tunjid.heron.data.repository

import app.bsky.actor.Type
import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetAuthorFeedFilter
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetFeedQueryParams
import app.bsky.feed.GetFeedResponse
import app.bsky.feed.GetListFeedQueryParams
import app.bsky.feed.GetListFeedResponse
import app.bsky.feed.GetPostThreadQueryParams
import app.bsky.feed.GetPostThreadResponseThreadUnion
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import app.bsky.feed.PostReplyRef
import app.bsky.richtext.Facet
import app.bsky.richtext.FacetByteSlice
import app.bsky.richtext.FacetFeatureUnion.Link
import app.bsky.richtext.FacetFeatureUnion.Mention
import app.bsky.richtext.FacetFeatureUnion.Tag
import app.bsky.richtext.FacetLink
import app.bsky.richtext.FacetMention
import app.bsky.richtext.FacetTag
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.daos.upsert
import com.tunjid.heron.data.database.entities.ThreadedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.withRefresh
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.BlueskyJson
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.JsonContent
import sh.christian.ozone.api.response.AtpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.Repost as BskyRepost
import sh.christian.ozone.api.Uri as BskyUri

class TimelineQuery(
    override val data: CursorQuery.Data,
    val timeline: Timeline,
) : CursorQuery {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TimelineQuery

        if (data != other.data) return false
        if (timeline.sourceId != other.timeline.sourceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + timeline.sourceId.hashCode()
        return result
    }

}

interface TimelineRepository {
    fun homeTimelines(): Flow<List<Timeline.Home>>

    fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean>

    fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

    fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>>

    suspend fun sendInteraction(
        interaction: Post.Interaction,
    )

    suspend fun createPost(
        request: Post.Create.Request,
        replyTo: Post.Create.Reply?,
    )
}

@Inject
class OfflineTimelineRepository(
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val transactionWriter: TransactionWriter,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : TimelineRepository {

    override fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>> = when (val timeline = query.timeline) {
        is Timeline.Home.Following -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getTimeline(
                        GetTimelineQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetTimelineResponse::cursor,
                networkFeed = GetTimelineResponse::feed,
            )
        )

        is Timeline.Home.Feed -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getFeed(
                        GetFeedQueryParams(
                            feed = AtUri(timeline.feedUri.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetFeedResponse::cursor,
                networkFeed = GetFeedResponse::feed,
            )
        )

        is Timeline.Home.List -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getListFeed(
                        GetListFeedQueryParams(
                            list = AtUri(timeline.listUri.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetListFeedResponse::cursor,
                networkFeed = GetListFeedResponse::feed,
            )
        )

        is Timeline.Profile.Media -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = query.data.limit,
                            cursor = cursor.value,
                            filter = GetAuthorFeedFilter.PostsWithMedia,
                        )
                    )
                },
                nextCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )

        is Timeline.Profile.Posts -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = query.data.limit,
                            cursor = cursor.value,
                            filter = GetAuthorFeedFilter.PostsNoReplies,
                        )
                    )
                },
                nextCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )

        is Timeline.Profile.Replies -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = query.data.limit,
                            cursor = cursor.value,
                            filter = GetAuthorFeedFilter.PostsWithReplies,
                        )
                    )
                },
                nextCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )
    }

    override fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean> = when (timeline) {
        is Timeline.Home.Feed -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getFeed(
                    GetFeedQueryParams(
                        feed = AtUri(timeline.feedUri.uri),
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetFeedResponse::feed,
        )

        is Timeline.Home.Following -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getTimeline(
                    GetTimelineQueryParams(
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetTimelineResponse::feed,
        )

        is Timeline.Home.List -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getListFeed(
                    GetListFeedQueryParams(
                        list = AtUri(timeline.listUri.uri),
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetListFeedResponse::feed,
        )

        is Timeline.Profile.Media -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 15.seconds,
            networkRequestBlock = {
                networkService.api.getAuthorFeed(
                    GetAuthorFeedQueryParams(
                        actor = Did(timeline.profileId.id),
                        limit = 1,
                        cursor = null,
                        filter = GetAuthorFeedFilter.PostsNoReplies,
                    )
                )
            },
            networkResponseToFeedViews = GetAuthorFeedResponse::feed,
        )

        is Timeline.Profile.Posts -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 15.seconds,
            networkRequestBlock = {
                networkService.api.getAuthorFeed(
                    GetAuthorFeedQueryParams(
                        actor = Did(timeline.profileId.id),
                        limit = 1,
                        cursor = null,
                        filter = GetAuthorFeedFilter.PostsNoReplies,
                    )
                )
            },
            networkResponseToFeedViews = GetAuthorFeedResponse::feed,
        )

        is Timeline.Profile.Replies -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 15.seconds,
            networkRequestBlock = {
                networkService.api.getAuthorFeed(
                    GetAuthorFeedQueryParams(
                        actor = Did(timeline.profileId.id),
                        limit = 1,
                        cursor = null,
                        filter = GetAuthorFeedFilter.PostsNoReplies,
                    )
                )
            },
            networkResponseToFeedViews = GetAuthorFeedResponse::feed,
        )
    }

    override fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>> =
        postDao.postEntitiesByUri(postUris = setOf(postUri))
            .mapNotNull { it.firstOrNull() }
            .take(1)
            .flatMapLatest { postEntity ->
                postDao.postThread(postId = postEntity.cid.id)
                    .map {
                        it.fold(
                            initial = emptyList(),
                            operation = ::spinThread,
                        )
                    }
            }
            .withRefresh {
                runCatchingWithNetworkRetry {
                    networkService.api.getPostThread(
                        GetPostThreadQueryParams(
                            uri = AtUri(postUri.uri)
                        )
                    )
                }
                    .getOrNull()
                    ?.thread
                    ?.let { thread ->
                        when (thread) {
                            is GetPostThreadResponseThreadUnion.BlockedPost -> Unit
                            is GetPostThreadResponseThreadUnion.NotFoundPost -> Unit
                            is GetPostThreadResponseThreadUnion.ThreadViewPost -> {
                                val authProfileId = savedStateRepository.signedInProfileId
                                if (authProfileId != null) multipleEntitySaverProvider
                                    .saveInTransaction {
                                        add(
                                            viewingProfileId = authProfileId,
                                            threadViewPost = thread.value,
                                        )
                                    }
                            }

                            is GetPostThreadResponseThreadUnion.Unknown -> Unit
                        }
                    }
            }


    override fun homeTimelines(): Flow<List<Timeline.Home>> =
        savedStateRepository.savedState
            .mapNotNull { it.preferences?.timelinePreferences }
            .distinctUntilChanged()
            .flatMapLatest { timelinePreferences ->
                timelinePreferences.mapIndexed { index, preference ->
                    when (Type.safeValueOf(preference.type)) {
                        Type.Feed -> timelineDao.feedGenerator(preference.value)
                            .filterNotNull()
                            .map {
                                Timeline.Home.Feed(
                                    name = it.displayName,
                                    feedUri = it.uri,
                                    position = index,
                                )
                            }

                        Type.List -> timelineDao.list(preference.value)
                            .filterNotNull()
                            .map {
                                Timeline.Home.List(
                                    listUri = it.uri,
                                    name = it.name,
                                    position = index,
                                )
                            }

                        Type.Timeline -> flowOf(
                            Timeline.Home.Following(
                                name = preference.value,
                                position = index,
                            )
                        )

                        is Type.Unknown -> emptyFlow()
                    }
                }
                    .merge()
                    .scan(emptyList<Timeline.Home>()) { timelines, timeline ->
                        // Add newest item first
                        (listOf(timeline) + timelines).distinctBy(Timeline.Home::sourceId)
                    }
                    .map { homeTimelines ->
                        homeTimelines.sortedBy(Timeline.Home::position)
                    }
                    .distinctUntilChangedBy {
                        it.joinToString(
                            separator = "-",
                            transform = Timeline.Home::name,
                        )
                    }
                    .filter(List<Timeline.Home>::isNotEmpty)
                    // Debounce for about 2 frames on a 60 hz display
                    .debounce(32)
            }

    override suspend fun createPost(
        request: Post.Create.Request,
        replyTo: Post.Create.Reply?,
    ) {
        val resolvedLinks: List<Post.Link> = coroutineScope {
            request.links.map { link ->
                async {
                    when (val target = link.target) {
                        is Post.LinkTarget.ExternalLink -> link
                        is Post.LinkTarget.UserDidMention -> link
                        is Post.LinkTarget.Hashtag -> link
                        is Post.LinkTarget.UserHandleMention -> {
                            profileDao.profiles(ids = listOf(target.handle))
                                .first()
                                .firstOrNull()
                                ?.let { profile ->
                                    link.copy(
                                        target = Post.LinkTarget.UserDidMention(profile.did)
                                    )
                                }
                        }
                    }
                }
            }.awaitAll()
        }.filterNotNull()

        val replyRef = replyTo?.let { original ->
            PostReplyRef(
                root = StrongRef(
                    uri = original.root.uri.uri.let(::AtUri),
                    cid = original.root.cid.id.let(::Cid),
                ),
                parent = StrongRef(
                    uri = original.parent.uri.uri.let(::AtUri),
                    cid = original.parent.cid.id.let(::Cid),
                ),
            )
        }

        val createRecordRequest = CreateRecordRequest(
            repo = request.authorId.id.let(::Did),
            collection = Nsid(PostCollection),
            record = BskyPost(
                text = request.text,
                reply = replyRef,
                facets = resolvedLinks.map { link ->
                    Facet(
                        index = FacetByteSlice(
                            byteStart = link.start.toLong(),
                            byteEnd = link.end.toLong(),
                        ),
                        features = when (val target = link.target) {
                            is Post.LinkTarget.ExternalLink -> listOf(
                                Link(FacetLink(target.uri.uri.let(::BskyUri)))
                            )

                            is Post.LinkTarget.UserDidMention -> listOf(
                                Mention(FacetMention(target.did.id.let(::Did)))
                            )

                            is Post.LinkTarget.Hashtag -> listOf(
                                Tag(FacetTag(target.tag))
                            )

                            is Post.LinkTarget.UserHandleMention -> emptyList()
                        },
                    )
                },
                createdAt = Clock.System.now(),
            )
                .asJsonContent(BskyPost.serializer()),
        )

        networkService.api.createRecord(createRecordRequest)
    }

    override suspend fun sendInteraction(
        interaction: Post.Interaction,
    ) {
        val authorId = savedStateRepository.signedInProfileId ?: return
        when (interaction) {
            is Post.Interaction.Create -> runCatchingWithNetworkRetry {
                networkService.api.createRecord(
                    CreateRecordRequest(
                        repo = authorId.id.let(::Did),
                        collection = Nsid(
                            when (interaction) {
                                is Post.Interaction.Create.Like -> LikeCollection
                                is Post.Interaction.Create.Repost -> RepostCollection
                            }
                        ),
                        record = when (interaction) {
                            is Post.Interaction.Create.Like -> BskyLike(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(BskyLike.serializer())

                            is Post.Interaction.Create.Repost -> BskyRepost(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(BskyRepost.serializer())
                        },
                    )
                )
            }
                .getOrNull()
                ?.let {
                    if (it.validationStatus !is CreateRecordValidationStatus.Valid) return@let
                    upsertInteraction(
                        partial = when (interaction) {
                            is Post.Interaction.Create.Like -> PostViewerStatisticsEntity.Partial.Like(
                                likeUri = it.uri.atUri.let(::Uri),
                                postId = interaction.postId,
                            )

                            is Post.Interaction.Create.Repost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = it.uri.atUri.let(::Uri),
                                postId = interaction.postId,
                            )
                        }
                    )
                }

            is Post.Interaction.Delete -> runCatchingWithNetworkRetry {
                networkService.api.deleteRecord(
                    DeleteRecordRequest(
                        repo = authorId.id.let(::Did),
                        collection = Nsid(
                            when (interaction) {
                                is Post.Interaction.Delete.RemoveRepost -> LikeCollection
                                is Post.Interaction.Delete.Unlike -> RepostCollection
                            }
                        ),
                        rkey = when (interaction) {
                            is Post.Interaction.Delete.RemoveRepost -> interaction.repostUri.uri
                            is Post.Interaction.Delete.Unlike -> interaction.likeUri.uri
                        }
                    )
                )
            }
                .getOrNull()
                ?.let {
                    upsertInteraction(
                        partial = when (interaction) {
                            is Post.Interaction.Delete.Unlike -> PostViewerStatisticsEntity.Partial.Like(
                                likeUri = null,
                                postId = interaction.postId,
                            )

                            is Post.Interaction.Delete.RemoveRepost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = null,
                                postId = interaction.postId,
                            )
                        }
                    )
                }
        }

    }

    private fun <NetworkResponse : Any> nextCursorFlow(
        query: TimelineQuery,
        currentCursor: Cursor,
        currentRequestWithNextCursor: suspend () -> AtpResponse<NetworkResponse>,
        nextCursor: NetworkResponse.() -> String?,
        networkFeed: NetworkResponse.() -> List<FeedViewPost>,
    ): Flow<Cursor> = com.tunjid.heron.data.utilities.nextCursorFlow(
        currentCursor = currentCursor,
        currentRequestWithNextCursor = currentRequestWithNextCursor,
        nextCursor = nextCursor,
        onResponse = {
            val authProfileId = savedStateRepository.signedInProfileId
            if (authProfileId != null) multipleEntitySaverProvider.saveInTransaction {
                if (timelineDao.isFirstRequest(query)) {
//                    timelineDao.deleteAllFeedsFor(query.timeline.sourceId)
                    timelineDao.upsertFeedFetchKey(
                        TimelineFetchKeyEntity(
                            sourceId = query.timeline.sourceId,
                            lastFetchedAt = query.data.cursorAnchor,
                            filterDescription = null,
                        )
                    )
                }
                add(
                    viewingProfileId = authProfileId,
                    timeline = query.timeline,
                    feedViewPosts = networkFeed(),
                )
            }
        }
    )

    private fun <T : Any> pollForTimelineUpdates(
        timeline: Timeline,
        pollInterval: Duration,
        networkRequestBlock: suspend () -> AtpResponse<T>,
        networkResponseToFeedViews: (T) -> List<FeedViewPost>,
    ) = flow {
        while (true) {
            val pollInstant = Clock.System.now()
            runCatchingWithNetworkRetry { networkRequestBlock() }
                .getOrNull()
                ?.let(networkResponseToFeedViews)
                ?.let { fetchedFeedViewPosts ->
                    val authProfileId = savedStateRepository.signedInProfileId
                    if (authProfileId != null) multipleEntitySaverProvider.saveInTransaction {
                        add(
                            viewingProfileId = authProfileId,
                            timeline = timeline,
                            feedViewPosts = fetchedFeedViewPosts,
                        )
                    }
                }
                ?: continue

            emit(pollInstant)
            delay(pollInterval.inWholeMilliseconds)
        }
    }
        .flatMapLatest { pollInstant ->
            combine(
                timelineDao.lastFetchKey(timeline.sourceId)
                    .map { it?.lastFetchedAt ?: pollInstant }
                    .distinctUntilChangedBy(Instant::toEpochMilliseconds)
                    .flatMapLatest {
                        timelineDao.feedItems(
                            sourceId = timeline.sourceId,
                            before = it,
                            limit = 1,
                            offset = 0,
                        )
                    },
                timelineDao.feedItems(
                    sourceId = timeline.sourceId,
                    before = pollInstant,
                    limit = 1,
                    offset = 0,
                )
            ) { latestSeen, latestSaved ->
                latestSaved
                    .firstOrNull()
                    ?.id != latestSeen.firstOrNull()?.id
            }
        }

    private fun observeAndRefreshTimeline(
        query: TimelineQuery,
        nextCursorFlow: Flow<Cursor>,
    ): Flow<CursorList<TimelineItem>> =
        combine(
            observeTimeline(query),
            nextCursorFlow,
            ::CursorList,
        )

    private fun observeTimeline(
        query: TimelineQuery,
    ): Flow<List<TimelineItem>> =
        timelineDao.feedItems(
            sourceId = query.timeline.sourceId,
            before = query.data.cursorAnchor,
            offset = query.data.page * query.data.limit,
            limit = query.data.limit,
        )
            .flatMapLatest { itemEntities ->
                combine(
                    postDao.posts(
                        itemEntities.flatMap {
                            listOfNotNull(
                                it.postId,
                                it.reply?.parentPostId,
                                it.reply?.rootPostId
                            )
                        }
                            .toSet()
                    ),
                    postDao.embeddedPosts(
                        itemEntities.map { it.postId }.toSet()
                    ),
                    profileDao.profiles(
                        itemEntities.mapNotNull { it.reposter }
                    )
                ) { posts, embeddedPosts, repostProfiles ->
                    val idsToPosts = posts.associateBy { it.entity.cid }
                    val idsToEmbeddedPosts = embeddedPosts.associateBy { it.postId }
                    val idsToRepostProfiles = repostProfiles.associateBy { it.did }

                    itemEntities.map { entity ->
                        val mainPost = idsToPosts.getValue(entity.postId)
                        val replyParent = entity.reply?.let { idsToPosts[it.parentPostId] }
                        val replyRoot = entity.reply?.let { idsToPosts[it.rootPostId] }
                        val repostedBy = entity.reposter?.let { idsToRepostProfiles[it] }

                        when {
                            replyRoot != null && replyParent != null -> TimelineItem.Thread(
                                id = entity.id,
                                generation = null,
                                anchorPostIndex = 2,
                                hasBreak = entity.reply?.grandParentPostAuthorId != null,
                                posts = listOf(
                                    replyRoot.asExternalModel(
                                        quote = idsToEmbeddedPosts[replyRoot.entity.cid]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    ),
                                    replyParent.asExternalModel(
                                        quote = idsToEmbeddedPosts[replyParent.entity.cid]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    ),
                                    mainPost.asExternalModel(
                                        quote = idsToEmbeddedPosts[entity.postId]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    )
                                ),
                            )

                            repostedBy != null -> TimelineItem.Repost(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                                by = repostedBy.asExternalModel(),
                                at = entity.indexedAt,
                            )

                            entity.isPinned -> TimelineItem.Pinned(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )

                            else -> TimelineItem.Single(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )
                        }
                    }
                }
            }

    private fun spinThread(
        list: List<TimelineItem.Thread>,
        thread: ThreadedPopulatedPostEntity,
    ) = when {
        list.isEmpty() || thread.generation == 0L -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(
                thread.entity.asExternalModel(
                    quote = null
                )
            )
        )

        thread.generation <= -1L -> list.dropLast(1) + list.last().let {
            it.copy(
                posts = it.posts + thread.entity.asExternalModel(quote = null)
            )
        }

        list.last().posts.first().cid != thread.rootPostId -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(
                thread.entity.asExternalModel(
                    quote = null
                )
            )
        )

        else -> list.dropLast(1) + list.last().let {
            it.copy(
                posts = it.posts + thread.entity.asExternalModel(quote = null)
            )
        }
    }

    private suspend fun upsertInteraction(
        partial: PostViewerStatisticsEntity.Partial,
    ) = transactionWriter.inTransaction {
        upsert(
            items = listOf(partial.asFull()),
            entityMapper = { listOf(partial) },
            insertMany = postDao::insertOrIgnorePostStatistics,
            updateMany = {
                when (partial) {
                    is PostViewerStatisticsEntity.Partial.Like ->
                        postDao.updatePostStatisticsLikes(listOf(partial))

                    is PostViewerStatisticsEntity.Partial.Repost ->
                        postDao.updatePostStatisticsReposts(listOf(partial))
                }
            }
        )
    }
}

private suspend fun TimelineDao.isFirstRequest(query: TimelineQuery): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(query.timeline.sourceId).first()?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.cursorAnchor.toEpochMilliseconds()
}

private fun <T> T.asJsonContent(
    serializer: KSerializer<T>,
): JsonContent = BlueskyJson.decodeFromString(
    BlueskyJson.encodeToString(serializer, this)
)

private const val PostCollection = "app.bsky.feed.post"

private const val RepostCollection = "app.bsky.feed.repost"

private const val LikeCollection = "app.bsky.feed.like"