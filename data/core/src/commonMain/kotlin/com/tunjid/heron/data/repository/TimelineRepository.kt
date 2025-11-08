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

import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import app.bsky.actor.SavedFeed
import app.bsky.actor.SavedFeedsPrefV2
import app.bsky.actor.Type
import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetActorLikesQueryParams
import app.bsky.feed.GetActorLikesResponse
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
import app.bsky.feed.Token
import app.bsky.labeler.GetServicesQueryParams
import app.bsky.labeler.GetServicesResponseViewUnion
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LabelerPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.labelVisibilitiesToDefinitions
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedLabelerEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.ThreadedPostEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.preferredPresentationPartial
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.TidGenerator
import com.tunjid.heron.data.utilities.lookupProfileDid
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toFlowOrEmpty
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

sealed interface TimelineRequest {

    data object Following : TimelineRequest

    data class OfProfile(
        val profileHandleOrDid: Id.Profile,
        val type: Timeline.Profile.Type,
    ) : TimelineRequest

    sealed interface OfFeed : TimelineRequest {
        data class WithUri(
            val uri: FeedGeneratorUri,
        ) : OfFeed

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val feedUriSuffix: String,
        ) : OfFeed
    }

    sealed interface OfList : TimelineRequest {
        data class WithUri(
            val uri: ListUri,
        ) : OfList

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val listUriSuffix: String,
        ) : OfList
    }

    sealed interface OfStarterPack : OfList {
        data class WithUri(
            val uri: StarterPackUri,
        ) : OfStarterPack

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val starterPackUriSuffix: String,
        ) : OfStarterPack
    }
}

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

    val preferences: Flow<Preferences>

    val labelers: Flow<List<Labeler>>

    val homeTimelines: Flow<List<Timeline.Home>>

    fun timeline(
        request: TimelineRequest,
    ): Flow<Timeline>

    fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean>

    fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

    fun postThreadedItems(
        postUri: PostUri,
    ): Flow<List<TimelineItem>>

    suspend fun updatePreferredPresentation(
        timeline: Timeline,
        presentation: Timeline.Presentation,
    ): Outcome

    suspend fun updateHomeTimelines(
        update: Timeline.Update,
    ): Outcome
}

@Inject
internal class OfflineTimelineRepository(
    @Named("AppScope") appScope: CoroutineScope,
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val labelDao: LabelDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
    private val tidGenerator: TidGenerator,
    private val authRepository: AuthRepository,
) : TimelineRepository {

    override val preferences: Flow<Preferences>
        get() = savedStateDataSource.savedState
            .map(SavedState::signedProfilePreferencesOrDefault)

    override val labelers: Flow<List<Labeler>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            savedStateDataSource.savedState.map {
                it.signedInProfileData
                    ?.preferences
                    ?.labelerPreferences
                    ?.map(LabelerPreference::labelerId)
                    ?.plus(Collections.DefaultLabelerProfileId)
                    ?: listOf(Collections.DefaultLabelerProfileId)
            }
                .distinctUntilChanged()
                .flatMapLatest { labelerIds ->
                    labelDao.labelers(labelerIds)
                        .map { it.map(PopulatedLabelerEntity::asExternalModel) }
                        .withRefresh {
                            networkService.runCatchingWithMonitoredNetworkRetry {
                                getServices(
                                    GetServicesQueryParams(
                                        dids = labelerIds.map { Did(it.id) },
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

    override fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>> = when (val timeline = query.timeline) {
        is Timeline.Home.Following -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = networkService.nextTimelineCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    getTimeline(
                        GetTimelineQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                nextCursor = GetTimelineResponse::cursor,
                networkFeed = GetTimelineResponse::feed,
            ),
        )

        is Timeline.Home.Feed -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = networkService.nextTimelineCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    getFeed(
                        GetFeedQueryParams(
                            feed = AtUri(timeline.source.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                nextCursor = GetFeedResponse::cursor,
                networkFeed = GetFeedResponse::feed,
            ),
        )

        is Timeline.Home.List -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = networkService.nextTimelineCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    getListFeed(
                        GetListFeedQueryParams(
                            list = AtUri(timeline.source.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                nextCursor = GetListFeedResponse::cursor,
                networkFeed = GetListFeedResponse::feed,
            ),
        )

        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Likes -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = networkService.nextTimelineCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorLikes(
                            GetActorLikesQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetActorLikesResponse::cursor,
                    networkFeed = GetActorLikesResponse::feed,
                ),
            )

            Timeline.Profile.Type.Media -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = networkService.nextTimelineCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithMedia,
                            ),
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                ),
            )

            Timeline.Profile.Type.Posts -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = networkService.nextTimelineCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsNoReplies,
                            ),
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                ),
            )

            Timeline.Profile.Type.Replies -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = networkService.nextTimelineCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithReplies,
                            ),
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                ),
            )

            Timeline.Profile.Type.Videos -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = networkService.nextTimelineCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithVideo,
                            ),
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                ),
            )
        }

        is Timeline.StarterPack -> timelineItems(
            query = TimelineQuery(
                data = query.data,
                timeline = timeline.listTimeline,
            ),
            cursor = cursor,
        )
    }
        .distinctUntilChanged()

    override fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean> = when (timeline) {
        is Timeline.Home.Feed -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                getFeed(
                    GetFeedQueryParams(
                        feed = AtUri(timeline.source.uri),
                        limit = 1,
                        cursor = null,
                    ),
                )
            },
            networkResponseToFeedViews = GetFeedResponse::feed,
        )

        is Timeline.Home.Following -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                getTimeline(
                    GetTimelineQueryParams(
                        limit = 1,
                        cursor = null,
                    ),
                )
            },
            networkResponseToFeedViews = GetTimelineResponse::feed,
        )

        is Timeline.Home.List -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                getListFeed(
                    GetListFeedQueryParams(
                        list = AtUri(timeline.source.uri),
                        limit = 1,
                        cursor = null,
                    ),
                )
            },
            networkResponseToFeedViews = GetListFeedResponse::feed,
        )

        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Likes -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    getActorLikes(
                        GetActorLikesQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                        ),
                    )
                },
                networkResponseToFeedViews = GetActorLikesResponse::feed,
            )

            Timeline.Profile.Type.Media -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithMedia,
                        ),
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Posts -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsNoReplies,
                        ),
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Replies -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithReplies,
                        ),
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Videos -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithVideo,
                        ),
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )
        }

        is Timeline.StarterPack -> hasUpdates(
            timeline = timeline.listTimeline,
        )
    }

    override fun postThreadedItems(
        postUri: PostUri,
    ): Flow<List<TimelineItem>> = savedStateDataSource.singleSessionFlow { signedInProfileId ->
        savedStateDataSource.savedState
            .map { it.signedProfilePreferencesOrDefault().contentLabelPreferences }
            .distinctUntilChanged()
            .flatMapLatest { contentLabelPreferences ->
                postDao.postEntitiesByUri(
                    viewingProfileId = signedInProfileId?.id,
                    postUris = setOf(postUri),
                )
                    .mapNotNull(List<PostEntity>::firstOrNull)
                    .take(1)
                    .flatMapLatest { postEntity ->
                        postDao.postThread(
                            postUri = postEntity.uri.uri,
                        )
                            .flatMapLatest { postThread ->
                                val postUris = postThread.mapTo(
                                    destination = mutableSetOf(),
                                    transform = { it.entity.uri },
                                )
                                val embeddedRecordUris = postThread.mapNotNullTo(
                                    destination = mutableSetOf(),
                                    transform = {
                                        it.entity.record?.embeddedRecordUri
                                    },
                                )
                                combine(
                                    flow = postDao.posts(
                                        viewingProfileId = signedInProfileId?.id,
                                        postUris = postUris,
                                    )
                                        .distinctUntilChanged(),
                                    flow2 = records(
                                        uris = embeddedRecordUris,
                                        viewingProfileId = signedInProfileId,
                                    ),
                                    flow3 = labelers,
                                    transform = { posts, embeddedRecords, labelers ->
                                        val urisToPosts = posts.associateBy { it.entity.uri }
                                        val recordUrisToEmbeddedRecords =
                                            embeddedRecords.associateBy {
                                                it.reference.uri
                                            }

                                        postThread.fold(
                                            initial = emptyList<TimelineItem.Thread>(),
                                            operation = { list, thread ->
                                                val populatedPostEntity =
                                                    urisToPosts.getValue(thread.entity.uri)
                                                val embeddedRecord = thread.entity
                                                    .record
                                                    ?.embeddedRecordUri
                                                    ?.let(recordUrisToEmbeddedRecords::get)
                                                val post = populatedPostEntity.asExternalModel(
                                                    embeddedRecord = embeddedRecord,
                                                )
                                                spinThread(
                                                    list = list,
                                                    thread = thread,
                                                    post = post,
                                                    labelers = labelers,
                                                    labelPreferences = contentLabelPreferences,
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                    }
                    .withRefresh {
                        networkService.runCatchingWithMonitoredNetworkRetry {
                            getPostThread(
                                GetPostThreadQueryParams(
                                    uri = AtUri(postUri.uri),
                                ),
                            )
                        }
                            .getOrNull()
                            ?.thread
                            ?.let { thread ->
                                when (thread) {
                                    is GetPostThreadResponseThreadUnion.BlockedPost -> Unit
                                    is GetPostThreadResponseThreadUnion.NotFoundPost -> Unit
                                    is GetPostThreadResponseThreadUnion.ThreadViewPost -> {
                                        multipleEntitySaverProvider
                                            .saveInTransaction {
                                                add(
                                                    viewingProfileId = signedInProfileId,
                                                    threadViewPost = thread.value,
                                                )
                                            }
                                    }
                                    is GetPostThreadResponseThreadUnion.Unknown -> Unit
                                }
                            }
                    }
                    .distinctUntilChanged()
            }
    }

    override val homeTimelines: Flow<List<Timeline.Home>>
        get() = savedStateDataSource.singleSessionFlow { signedInProfileId ->
            savedStateDataSource.savedState
                .map { it.signedProfilePreferencesOrDefault().timelinePreferences }
                .distinctUntilChanged()
                .flatMapLatest { timelinePreferences ->
                    timelinePreferences.mapIndexed { index, preference ->
                        when (Type.safeValueOf(preference.type)) {
                            Type.Feed -> feedGeneratorTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = FeedGeneratorUri(preference.value),
                                position = index,
                                isPinned = preference.pinned,
                            )

                            Type.List -> listTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = ListUri(preference.value),
                                position = index,
                                isPinned = preference.pinned,
                            )

                            Type.Timeline -> followingTimeline(
                                signedInProfileId = signedInProfileId,
                                name = preference.value.replaceFirstChar(Char::titlecase),
                                position = index,
                                isPinned = preference.pinned,
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
                        .distinctUntilChangedBy { timelines ->
                            timelines.joinToString(
                                separator = "-",
                                transform = { "${it.name}-${it.presentation.key}-${it.isPinned}" },
                            )
                        }
                        .filter(List<Timeline.Home>::isNotEmpty)
                        .debounce { timelines ->
                            if (timelines.size == timelinePreferences.size) 0.seconds
                            else 3.seconds
                        }
                }
                .distinctUntilChanged()
        }

    override fun timeline(
        request: TimelineRequest,
    ): Flow<Timeline> = savedStateDataSource.singleSessionFlow { signedInProfileId ->
        savedStateDataSource.savedState
            .map { it.signedProfilePreferencesOrDefault().timelinePreferences }
            .distinctUntilChanged()
            .flatMapLatest { preferences ->
                flow {
                    when (request) {
                        is TimelineRequest.OfFeed.WithUri -> emitAll(
                            feedGeneratorTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = request.uri,
                                position = 0,
                                isPinned = preferences.firstOrNull {
                                    it.value == request.uri.uri
                                }?.pinned ?: false,
                            ),
                        )

                        is TimelineRequest.OfList.WithUri -> emitAll(
                            listTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = request.uri,
                                position = 0,
                                isPinned = preferences.firstOrNull {
                                    it.value == request.uri.uri
                                }?.pinned ?: false,
                            ),
                        )

                        is TimelineRequest.OfFeed.WithProfile -> {
                            val profileDid = lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                                profileDao = profileDao,
                                networkService = networkService,
                            ) ?: return@flow
                            val uri = FeedGeneratorUri(
                                uri = "at://${profileDid.did}/${Collections.FeedGenerator}/${request.feedUriSuffix}",
                            )
                            emitAll(
                                feedGeneratorTimeline(
                                    signedInProfileId = signedInProfileId,
                                    uri = uri,
                                    position = 0,
                                    isPinned = preferences.firstOrNull {
                                        it.value == uri.uri
                                    }?.pinned ?: false,
                                ),
                            )
                        }

                        is TimelineRequest.OfList.WithProfile -> {
                            val profileDid = lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                                profileDao = profileDao,
                                networkService = networkService,
                            ) ?: return@flow
                            val uri = ListUri(
                                uri = "at://${profileDid.did}/${Collections.List}/${request.listUriSuffix}",
                            )
                            emitAll(
                                listTimeline(
                                    signedInProfileId = signedInProfileId,
                                    uri = uri,
                                    position = 0,
                                    isPinned = preferences.firstOrNull {
                                        it.value == uri.uri
                                    }?.pinned ?: false,
                                ),
                            )
                        }

                        is TimelineRequest.OfProfile -> emitAll(
                            profileTimeline(
                                signedInProfileId = signedInProfileId,
                                profileHandleOrDid = request.profileHandleOrDid,
                                type = request.type,
                            ),
                        )

                        is TimelineRequest.OfStarterPack.WithProfile -> {
                            val profileDid = lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                                profileDao = profileDao,
                                networkService = networkService,
                            ) ?: return@flow
                            val uri = StarterPackUri(
                                uri = "at://${profileDid.did}/${Collections.StarterPack}/${request.starterPackUriSuffix}",
                            )
                            emitAll(
                                starterPackTimeline(
                                    signedInProfileId = signedInProfileId,
                                    uri = uri,
                                ),
                            )
                        }

                        is TimelineRequest.OfStarterPack.WithUri -> emitAll(
                            starterPackTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = request.uri,
                            ),
                        )

                        TimelineRequest.Following -> emitAll(
                            followingTimeline(
                                // TODO: Get a string resource for this
                                name = "",
                                signedInProfileId = signedInProfileId,
                                position = 0,
                                isPinned = preferences.firstOrNull {
                                    Type.safeValueOf(it.type) is Type.Timeline
                                }?.pinned ?: false,
                            ),
                        )
                    }
                }
            }
    }

    override suspend fun updatePreferredPresentation(
        timeline: Timeline,
        presentation: Timeline.Presentation,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        runCatchingUnlessCancelled {
            timelineDao.updatePreferredTimelinePresentation(
                partial = preferredPresentationPartial(
                    signedInProfileId = signedInProfileId,
                    sourceId = timeline.sourceId,
                    presentation = presentation,
                ),
            )
        }.toOutcome()
    } ?: expiredSessionOutcome()

    override suspend fun updateHomeTimelines(
        update: Timeline.Update,
    ): Outcome = networkService.runCatchingWithMonitoredNetworkRetry {
        getPreferences()
    }
        .fold(
            onSuccess = { preferencesResponse ->
                networkService.runCatchingWithMonitoredNetworkRetry {
                    putPreferences(
                        PutPreferencesRequest(
                            preferences = preferencesResponse.preferences.updateFeed {
                                updateFeedPreferencesFrom(
                                    tidGenerator = tidGenerator,
                                    update = update,
                                )
                            },
                        ),
                    )
                }.fold(
                    onSuccess = { authRepository.updateSignedInUser() },
                    onFailure = Outcome::Failure,
                )
            },
            onFailure = Outcome::Failure,
        )

    private fun <NetworkResponse : Any> NetworkService.nextTimelineCursorFlow(
        query: TimelineQuery,
        currentCursor: Cursor,
        currentRequestWithNextCursor: suspend BlueskyApi.() -> AtpResponse<NetworkResponse>,
        nextCursor: NetworkResponse.() -> String?,
        networkFeed: NetworkResponse.() -> List<FeedViewPost>,
    ): Flow<Cursor> = savedStateDataSource
        .singleSessionFlow { signedInProfileId ->
            nextCursorFlow(
                currentCursor = currentCursor,
                currentRequestWithNextCursor = currentRequestWithNextCursor,
                nextCursor = nextCursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        if (timelineDao.isFirstRequest(signedInProfileId, query)) {
                            timelineDao.deleteAllFeedsFor(query.timeline.sourceId)
                            timelineDao.insertOrPartiallyUpdateTimelineFetchedAt(
                                listOf(
                                    TimelinePreferencesEntity(
                                        viewingProfileId = signedInProfileId,
                                        sourceId = query.timeline.sourceId,
                                        lastFetchedAt = query.data.cursorAnchor,
                                        preferredPresentation = null,
                                    ),
                                ),
                            )
                        }
                        add(
                            viewingProfileId = signedInProfileId,
                            timeline = query.timeline,
                            feedViewPosts = networkFeed(),
                        )
                    }
                },
            )
        }

    private fun <T : Any> pollForTimelineUpdates(
        timeline: Timeline,
        pollInterval: Duration,
        networkRequestBlock: suspend BlueskyApi.() -> AtpResponse<T>,
        networkResponseToFeedViews: (T) -> List<FeedViewPost>,
    ) = savedStateDataSource.singleSessionFlow { signedInProfileId ->
        flow {
            while (true) {
                val pollInstant = Clock.System.now()
                val succeeded = networkService.runCatchingWithMonitoredNetworkRetry(
                    block = networkRequestBlock,
                )
                    .getOrNull()
                    ?.let(networkResponseToFeedViews)
                    ?.let { fetchedFeedViewPosts ->
                        multipleEntitySaverProvider.saveInTransaction {
                            add(
                                viewingProfileId = signedInProfileId,
                                timeline = timeline,
                                feedViewPosts = fetchedFeedViewPosts,
                            )
                        }
                    } != null

                if (succeeded) emit(signedInProfileId to pollInstant)
                delay(pollInterval.inWholeMilliseconds)
            }
        }
    }
        .flatMapLatest { (signedInProfileId, pollInstant) ->
            combine(
                timelineDao.lastFetchKey(
                    viewingProfileId = signedInProfileId?.id,
                    sourceId = timeline.sourceId,
                )
                    .map { it?.lastFetchedAt ?: pollInstant }
                    .distinctUntilChangedBy(Instant::toEpochMilliseconds)
                    .flatMapLatest {
                        timelineDao.feedItems(
                            viewingProfileId = signedInProfileId?.id,
                            sourceId = timeline.sourceId,
                            before = it,
                            limit = 1,
                            offset = 0,
                        )
                    },
                timelineDao.feedItems(
                    viewingProfileId = signedInProfileId?.id,
                    sourceId = timeline.sourceId,
                    before = pollInstant,
                    limit = 1,
                    offset = 0,
                ),
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
            flow = observeTimeline(query),
            flow2 = nextCursorFlow,
            transform = ::CursorList,
        )

    private fun observeTimeline(
        query: TimelineQuery,
    ): Flow<List<TimelineItem>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            savedStateDataSource.savedState
                .map { it.signedProfilePreferencesOrDefault().contentLabelPreferences }
                .distinctUntilChanged()
                .flatMapLatest { contentLabelPreferences ->
                    val labelsVisibilityMap = contentLabelPreferences.associateBy(
                        keySelector = ContentLabelPreference::label,
                        valueTransform = ContentLabelPreference::visibility,
                    )
                    timelineDao.feedItems(
                        viewingProfileId = signedInProfileId?.id,
                        sourceId = query.timeline.sourceId,
                        before = query.data.cursorAnchor,
                        offset = query.data.offset,
                        limit = query.data.limit,
                    )
                        .distinctUntilChanged()
                        .flatMapLatest latestFeedItems@{ itemEntities ->
                            if (itemEntities.isEmpty()) return@latestFeedItems emptyFlow()

                            val postIds = itemEntities.flatMap {
                                listOfNotNull(
                                    it.postUri,
                                    it.reply?.parentPostUri,
                                    it.reply?.rootPostUri,
                                )
                            }
                                .toSet()
                            val profileIds = itemEntities.mapNotNullTo(
                                destination = mutableSetOf(),
                                transform = TimelineItemEntity::reposter,
                            )
                            val embeddedRecordUris = itemEntities.mapNotNullTo(
                                destination = mutableSetOf(),
                                transform = TimelineItemEntity::embeddedRecordUri,
                            )
                            combine(
                                flow = postIds.toFlowOrEmpty { ids ->
                                    postDao.posts(
                                        viewingProfileId = signedInProfileId?.id,
                                        postUris = ids,
                                    )
                                },
                                flow2 = records(
                                    embeddedRecordUris,
                                    signedInProfileId,
                                ),
                                flow3 = profileIds.toFlowOrEmpty(
                                    block = profileDao::profiles,
                                ),
                                flow4 = labelers,
                            ) { posts, embeddedRecords, repostProfiles, labelers ->
                                if (posts.isEmpty()) return@combine emptyList()
                                val urisToPosts = posts.associateBy { it.entity.uri }
                                val recordUrisToEmbeddedRecords = embeddedRecords.associateBy {
                                    it.reference.uri
                                }
                                val idsToRepostProfiles = repostProfiles.associateBy {
                                    it.entity.did
                                }

                                itemEntities.mapNotNull { entity ->
                                    val mainPost = urisToPosts[entity.postUri]
                                        ?.asExternalModel(
                                            embeddedRecord = entity.embeddedRecordUri
                                                ?.let(recordUrisToEmbeddedRecords::get),
                                        ) ?: return@mapNotNull null

                                    val postLabels = when {
                                        mainPost.labels.isEmpty() -> emptySet()
                                        else -> mainPost.labels.mapTo(
                                            destination = mutableSetOf(),
                                            transform = Label::value,
                                        )
                                    }

                                    // Check for global hidden label
                                    if (postLabels.contains(Label.Hidden)) return@mapNotNull null

                                    // Check for global non authenticated label
                                    val isSignedIn = signedInProfileId != null
                                    if (!isSignedIn && postLabels.contains(Label.NonAuthenticated)) return@mapNotNull null

                                    val visibilitiesToDefinitions = labelVisibilitiesToDefinitions(
                                        postLabels = postLabels,
                                        labelers = labelers,
                                        labelsVisibilityMap = labelsVisibilityMap,
                                    )

                                    val shouldHide = visibilitiesToDefinitions.getOrElse(
                                        key = Label.Visibility.Hide,
                                        defaultValue = ::emptyList,
                                    ).isNotEmpty()

                                    if (shouldHide) return@mapNotNull null

                                    val replyParent =
                                        entity.reply?.let { urisToPosts[it.parentPostUri] }
                                    val replyRoot =
                                        entity.reply?.let { urisToPosts[it.rootPostUri] }
                                    val repostedBy =
                                        entity.reposter?.let { idsToRepostProfiles[it] }

                                    when {
                                        replyRoot != null && replyParent != null -> TimelineItem.Thread(
                                            id = entity.id,
                                            generation = null,
                                            anchorPostIndex = 2,
                                            hasBreak = entity.reply?.grandParentPostAuthorId != null,
                                            labelVisibilitiesToDefinitions = visibilitiesToDefinitions,
                                            posts = listOf(
                                                replyRoot.asExternalModel(
                                                    embeddedRecord = replyRoot.entity
                                                        .record
                                                        ?.embeddedRecordUri
                                                        ?.let(recordUrisToEmbeddedRecords::get),
                                                ),
                                                replyParent.asExternalModel(
                                                    embeddedRecord = replyParent.entity
                                                        .record
                                                        ?.embeddedRecordUri
                                                        ?.let(recordUrisToEmbeddedRecords::get),
                                                ),
                                                mainPost,
                                            ),
                                        )

                                        repostedBy != null -> TimelineItem.Repost(
                                            id = entity.id,
                                            post = mainPost,
                                            by = repostedBy.asExternalModel(),
                                            at = entity.indexedAt,
                                            labelVisibilitiesToDefinitions = visibilitiesToDefinitions,
                                        )

                                        entity.isPinned -> TimelineItem.Pinned(
                                            id = entity.id,
                                            post = mainPost,
                                            labelVisibilitiesToDefinitions = visibilitiesToDefinitions,
                                        )

                                        else -> TimelineItem.Single(
                                            id = entity.id,
                                            post = mainPost,
                                            labelVisibilitiesToDefinitions = visibilitiesToDefinitions,
                                        )
                                    }
                                }
                            }
                                .filter(List<TimelineItem>::isNotEmpty)
                        }
                }
        }

    private fun followingTimeline(
        signedInProfileId: ProfileId?,
        name: String,
        position: Int,
        isPinned: Boolean,
    ): Flow<Timeline.Home.Following> = timelineDao.lastFetchKey(
        viewingProfileId = signedInProfileId?.id,
        sourceId = Constants.timelineFeed.uri,
    )
        .distinctUntilChanged()
        .map { timelinePreferenceEntity ->
            Timeline.Home.Following(
                name = name,
                position = position,
                lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                presentation = timelinePreferenceEntity.preferredPresentation(),
                isPinned = isPinned,
            )
        }

    private fun profileTimeline(
        signedInProfileId: ProfileId?,
        profileHandleOrDid: Id.Profile,
        type: Timeline.Profile.Type,
    ): Flow<Timeline.Profile> = profileDao.profiles(
        ids = listOf(profileHandleOrDid),
    )
        .mapNotNull(List<PopulatedProfileEntity>::firstOrNull)
        .distinctUntilChangedBy { it.entity.did }
        .flatMapLatest { populatedProfileEntity ->
            timelineDao.lastFetchKey(
                viewingProfileId = signedInProfileId?.id,
                sourceId = type.sourceId(populatedProfileEntity.entity.did),
            )
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Profile(
                        profileId = populatedProfileEntity.entity.did,
                        type = type,
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                    )
                }
        }

    private fun feedGeneratorTimeline(
        signedInProfileId: ProfileId?,
        uri: FeedGeneratorUri,
        position: Int,
        isPinned: Boolean,
    ): Flow<Timeline.Home.Feed> = feedGeneratorDao.feedGenerators(listOf(uri))
        .map(List<PopulatedFeedGeneratorEntity>::firstOrNull)
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { populatedFeedGeneratorEntity ->
            timelineDao.lastFetchKey(
                viewingProfileId = signedInProfileId?.id,
                sourceId = populatedFeedGeneratorEntity.entity.uri.uri,
            )
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Home.Feed(
                        position = position,
                        feedGenerator = populatedFeedGeneratorEntity.asExternalModel(),
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                        supportedPresentations = listOfNotNull(
                            Timeline.Presentation.Text.WithEmbed,
                            Timeline.Presentation.Media.Expanded.takeIf {
                                populatedFeedGeneratorEntity.entity.supportsMediaPresentation()
                            },
                            Timeline.Presentation.Media.Condensed.takeIf {
                                populatedFeedGeneratorEntity.entity.supportsMediaPresentation()
                            },
                            Timeline.Presentation.Media.Grid.takeIf {
                                populatedFeedGeneratorEntity.entity.supportsMediaPresentation()
                            },
                        ),
                        isPinned = isPinned,
                    )
                }
        }
        .withRefresh {
            networkService.refresh(
                uri = uri,
                savedStateDataSource = savedStateDataSource,
                multipleEntitySaverProvider = multipleEntitySaverProvider,
            )
        }

    private fun listTimeline(
        signedInProfileId: ProfileId?,
        uri: ListUri,
        position: Int,
        isPinned: Boolean,
    ): Flow<Timeline.Home.List> = listDao.list(uri.uri)
        .filterNotNull()
        .distinctUntilChangedBy(PopulatedListEntity::entity)
        .flatMapLatest {
            timelineDao.lastFetchKey(
                viewingProfileId = signedInProfileId?.id,
                sourceId = it.entity.uri.uri,
            )
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Home.List(
                        position = position,
                        feedList = it.asExternalModel(),
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                        isPinned = isPinned,
                    )
                }
        }
        .withRefresh {
            networkService.refresh(
                uri = uri,
                savedStateDataSource = savedStateDataSource,
                multipleEntitySaverProvider = multipleEntitySaverProvider,
            )
        }

    private fun starterPackTimeline(
        signedInProfileId: ProfileId?,
        uri: StarterPackUri,
    ): Flow<Timeline.StarterPack> = starterPackDao.starterPack(uri.uri)
        .mapNotNull { populatedStarterPackEntity ->
            populatedStarterPackEntity?.list?.let { populatedStarterPackEntity to it }
        }
        .filterNotNull()
        .distinctUntilChangedBy { it.first.entity }
        .flatMapLatest { (populatedStarterPackEntity, listEntity) ->
            listTimeline(
                signedInProfileId = signedInProfileId,
                uri = listEntity.uri,
                position = 0,
                isPinned = false,
            ).map { listTimeline ->
                Timeline.StarterPack(
                    starterPack = populatedStarterPackEntity.asExternalModel(),
                    listTimeline = listTimeline,
                )
            }
        }
        .withRefresh {
            networkService.refresh(
                uri = uri,
                savedStateDataSource = savedStateDataSource,
                multipleEntitySaverProvider = multipleEntitySaverProvider,
            )
        }

    private fun spinThread(
        labelers: List<Labeler>,
        labelPreferences: ContentLabelPreferences,
        list: List<TimelineItem.Thread>,
        thread: ThreadedPostEntity,
        post: Post,
    ) = when {
        // To start or for the OP, start a new thread
        list.isEmpty() || thread.generation == 0L -> list + TimelineItem.Thread(
            id = thread.entity.uri.uri,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(post),
            labelVisibilitiesToDefinitions = post.labelVisibilitiesToDefinitions(
                labelers = labelers,
                labelPreferences = labelPreferences,
            ),
        )
        // For parents, edit the head
        thread.generation <= -1L -> list.dropLast(1) + list.last().let {
            it.copy(posts = it.posts + post)
        }

        // New reply to the OP, start its own thread
        list.last().posts.first().uri != thread.rootPostUri -> list + TimelineItem.Thread(
            id = thread.entity.uri.uri,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(post),
            labelVisibilitiesToDefinitions = post.labelVisibilitiesToDefinitions(
                labelers = labelers,
                labelPreferences = labelPreferences,
            ),
        )

        // Just tack the post to the current thread
        else -> list.dropLast(1) + list.last().let {
            it.copy(posts = it.posts + post)
        }
    }

    private fun records(
        uris: Set<RecordUri>,
        viewingProfileId: ProfileId?,
    ): Flow<List<Record>> {
        val feedUris = LazyList<FeedGeneratorUri>()
        val listUris = LazyList<ListUri>()
        val postUris = LazyList<PostUri>()
        val starterPackUris = LazyList<StarterPackUri>()

        uris.forEach { uri ->
            when (uri) {
                is FeedGeneratorUri -> feedUris.add(uri)
                is ListUri -> listUris.add(uri)
                is PostUri -> postUris.add(uri)
                is StarterPackUri -> starterPackUris.add(uri)
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
        ) { feeds, lists, posts, starterPacks ->
            feeds + lists + posts + starterPacks
        }
    }
}

private fun TimelinePreferencesEntity?.preferredPresentation() =
    when (this?.preferredPresentation) {
        Timeline.Presentation.Media.Expanded.key -> Timeline.Presentation.Media.Expanded
        Timeline.Presentation.Media.Condensed.key -> Timeline.Presentation.Media.Condensed
        Timeline.Presentation.Media.Grid.key -> Timeline.Presentation.Media.Grid
        Timeline.Presentation.Text.WithEmbed.key -> Timeline.Presentation.Text.WithEmbed
        else -> Timeline.Presentation.Text.WithEmbed
    }

private suspend fun TimelineDao.isFirstRequest(
    signedInProfileId: ProfileId?,
    query: TimelineQuery,
): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(
        viewingProfileId = signedInProfileId?.id,
        sourceId = query.timeline.sourceId,
    ).first()?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.cursorAnchor.toEpochMilliseconds()
}

private suspend fun PreferencesUnion.SavedFeedsPrefV2.updateFeedPreferencesFrom(
    tidGenerator: TidGenerator,
    update: Timeline.Update,
): PreferencesUnion.SavedFeedsPrefV2 = PreferencesUnion.SavedFeedsPrefV2(
    SavedFeedsPrefV2(
        items = when (update) {
            is Timeline.Update.Bulk -> value.items.associateBy(
                keySelector = SavedFeed::value,
                valueTransform = SavedFeed::id,
            ).let { savedFeedValuesToIds ->
                update.timelines.mapNotNull { timeline ->
                    when (timeline) {
                        is Timeline.Home.Feed -> savedFeedValuesToIds[
                            timeline.feedGenerator.uri.uri,
                        ]?.let { id ->
                            SavedFeed(
                                id = id,
                                type = Type.Feed,
                                value = timeline.feedGenerator.uri.uri,
                                pinned = timeline.isPinned,
                            )
                        }

                        is Timeline.Home.Following -> savedFeedValuesToIds[
                            "following",
                        ]?.let { id ->
                            SavedFeed(
                                id = id,
                                type = Type.Timeline,
                                value = "following",
                                pinned = timeline.isPinned,
                            )
                        }

                        is Timeline.Home.List -> savedFeedValuesToIds[
                            timeline.feedList.uri.uri,
                        ]?.let { id ->
                            SavedFeed(
                                id = id,
                                type = Type.List,
                                value = timeline.feedList.uri.uri,
                                pinned = timeline.isPinned,
                            )
                        }
                    }
                }
            }

            is Timeline.Update.OfFeedGenerator.Pin -> value.items.filter {
                it.value != update.uri.uri
            }
                .partition(SavedFeed::pinned)
                .let { (pinned, saved) ->
                    pinned + SavedFeed(
                        id = tidGenerator.generate(),
                        type = Type.Feed,
                        value = update.uri.uri,
                        pinned = true,
                    ) + saved
                }

            is Timeline.Update.OfFeedGenerator.Remove -> value.items.filter { savedFeed ->
                if (savedFeed.type != Type.Feed) return@filter true
                savedFeed.value != update.uri.uri
            }

            is Timeline.Update.OfFeedGenerator.Save -> value.items.filter {
                it.value != update.uri.uri
            } + SavedFeed(
                id = tidGenerator.generate(),
                type = Type.Feed,
                value = update.uri.uri,
                pinned = false,
            )
        },
    ),
)

private inline fun List<PreferencesUnion>.updateFeed(
    block: PreferencesUnion.SavedFeedsPrefV2.() -> PreferencesUnion.SavedFeedsPrefV2,
): List<PreferencesUnion> =
    map { preference ->
        when (preference) {
            is PreferencesUnion.SavedFeedsPrefV2 -> preference.block()
            else -> preference
        }
    }

private fun FeedGeneratorEntity.supportsMediaPresentation() =
    when (contentMode) {
        Token.ContentModeVideo.value,
        "app.bsky.feed.defs#contentModeVideo",
        "app.bsky.feed.defs#contentModePhoto",
        "app.bsky.feed.defs#contentModeImage",
        "app.bsky.feed.defs#contentModeMedia",
        "com.tunjid.heron.defs#contentModeMedia",
        -> true

        else -> false
    }
