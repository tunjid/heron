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

import app.bsky.actor.PutPreferencesRequest
import app.bsky.actor.SavedFeedType
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
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.ThreadedPostEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.preferredPresentationPartial
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
    private val preferenceUpdater: PreferenceUpdater,
    private val profileLookup: ProfileLookup,
    private val recordResolver: RecordResolver,
    private val authRepository: AuthRepository,
) : TimelineRepository {

    override val preferences: Flow<Preferences>
        get() = savedStateDataSource.savedState
            .map(SavedState::signedProfilePreferencesOrDefault)

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
        postDao.postThread(
            postUri = postUri.uri,
        )
            .distinctUntilChanged()
            .flatMapLatest { postThread ->
                recordResolver.timelineItems(
                    items = postThread,
                    signedInProfileId = signedInProfileId,
                    postUri = {
                        it.entity.uri
                    },
                    associatedRecordUris = {
                        listOfNotNull(it.entity.record?.embeddedRecordUri)
                    },
                    associatedProfileIds = {
                        emptyList()
                    },
                    block = ::spinThread,
                )
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

    override val homeTimelines: Flow<List<Timeline.Home>>
        get() = savedStateDataSource.singleSessionFlow { signedInProfileId ->
            savedStateDataSource.savedState
                .map { it.signedProfilePreferencesOrDefault().timelinePreferences }
                .distinctUntilChanged()
                .flatMapLatest { timelinePreferences ->
                    timelinePreferences.mapIndexed { index, preference ->
                        when (SavedFeedType.safeValueOf(preference.type)) {
                            SavedFeedType.Feed -> feedGeneratorTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = FeedGeneratorUri(preference.value),
                                position = index,
                                isPinned = preference.pinned,
                            )

                            SavedFeedType.List -> listTimeline(
                                signedInProfileId = signedInProfileId,
                                uri = ListUri(preference.value),
                                position = index,
                                isPinned = preference.pinned,
                            )

                            SavedFeedType.Timeline -> followingTimeline(
                                signedInProfileId = signedInProfileId,
                                name = preference.value.replaceFirstChar(Char::titlecase),
                                position = index,
                                isPinned = preference.pinned,
                            )

                            is SavedFeedType.Unknown -> emptyFlow()
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
                            val profileDid = profileLookup.lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                            ) ?: return@flow
                            val uri = FeedGeneratorUri(
                                uri = "at://${profileDid.did}/${FeedGeneratorUri.NAMESPACE}/${request.feedUriSuffix}",
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
                            val profileDid = profileLookup.lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                            ) ?: return@flow
                            val uri = ListUri(
                                uri = "at://${profileDid.did}/${ListUri.NAMESPACE}/${request.listUriSuffix}",
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
                            val profileDid = profileLookup.lookupProfileDid(
                                profileId = request.profileHandleOrDid,
                            ) ?: return@flow
                            val uri = StarterPackUri(
                                uri = "at://${profileDid.did}/${StarterPackUri.NAMESPACE}/${request.starterPackUriSuffix}",
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
                                    SavedFeedType.safeValueOf(it.type) is SavedFeedType.Timeline
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
        getPreferencesForActor()
    }
        .fold(
            onSuccess = { preferencesResponse ->
                networkService.runCatchingWithMonitoredNetworkRetry {
                    putPreferences(
                        PutPreferencesRequest(
                            preferences = preferenceUpdater.update(
                                networkPreferences = preferencesResponse.preferences,
                                update = update,
                            ),
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

                    recordResolver.timelineItems(
                        items = itemEntities,
                        signedInProfileId = signedInProfileId,
                        postUri = TimelineItemEntity::postUri,
                        associatedRecordUris = {
                            listOfNotNull(
                                it.reply?.parentPostUri,
                                it.reply?.rootPostUri,
                                it.embeddedRecordUri,
                                it.reply?.rootPostEmbeddedRecordUri,
                                it.reply?.parentPostEmbeddedRecordUri,
                            )
                        },
                        associatedProfileIds = {
                            listOfNotNull(it.reposter)
                        },
                        block = { entity ->
                            val replyParent = entity.reply?.parentPostUri?.let(::record) as? Post
                            val replyRoot = entity.reply?.rootPostUri?.let(::record) as? Post
                            val repostedBy = entity.reposter?.let(::profile)

                            list += when {
                                replyRoot != null && replyParent != null -> TimelineItem.Thread(
                                    id = entity.id,
                                    generation = null,
                                    anchorPostIndex = 2,
                                    hasBreak = entity.reply?.grandParentPostAuthorId != null,
                                    appliedLabels = appliedLabels,
                                    signedInProfileId = signedInProfileId,
                                    posts = listOfNotNull(
                                        replyRoot,
                                        replyParent,
                                        post,
                                    ),
                                    postUrisToThreadGates = buildMap {
                                        put(replyRoot.uri, threadGate(replyRoot.uri))
                                        put(replyParent.uri, threadGate(replyParent.uri))
                                        put(post.uri, threadGate(post.uri))
                                    },
                                )

                                repostedBy != null -> TimelineItem.Repost(
                                    id = entity.id,
                                    post = post,
                                    by = repostedBy,
                                    at = entity.indexedAt,
                                    threadGate = threadGate(post.uri),
                                    appliedLabels = appliedLabels,
                                    signedInProfileId = signedInProfileId,
                                )

                                entity.isPinned -> TimelineItem.Pinned(
                                    id = entity.id,
                                    post = post,
                                    threadGate = threadGate(post.uri),
                                    appliedLabels = appliedLabels,
                                    signedInProfileId = signedInProfileId,
                                )

                                else -> TimelineItem.Single(
                                    id = entity.id,
                                    post = post,
                                    threadGate = threadGate(post.uri),
                                    appliedLabels = appliedLabels,
                                    signedInProfileId = signedInProfileId,
                                )
                            }
                        },
                    )
                        .filter(List<TimelineItem>::isNotEmpty)
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
        signedInProfiledId = signedInProfileId?.id,
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
            recordResolver.resolve(
                uri = uri,
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
            recordResolver.resolve(
                uri = uri,
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
            recordResolver.resolve(
                uri = uri,
            )
        }

    private fun spinThread(
        context: RecordResolver.TimelineItemCreationContext,
        thread: ThreadedPostEntity,
    ) = with(context) {
        val lastItem = list.lastOrNull()
        when {
            // To start or for the OP, start a new thread
            lastItem == null || thread.generation == 0L -> list += TimelineItem.Thread(
                id = thread.entity.uri.uri,
                generation = thread.generation,
                anchorPostIndex = 0,
                hasBreak = false,
                posts = listOf(post),
                appliedLabels = appliedLabels,
                signedInProfileId = signedInProfileId,
                postUrisToThreadGates = mapOf(post.uri to threadGate(post.uri)),
            )
            // For parents, edit the head
            thread.generation <= -1L ->
                if (lastItem is TimelineItem.Thread) list[list.lastIndex] = lastItem.copy(
                    posts = lastItem.posts + post,
                    postUrisToThreadGates = lastItem.postUrisToThreadGates + (
                        post.uri to threadGate(
                            post.uri,
                        )
                        ),
                )
                else Unit

            // New reply to the OP, start its own thread
            lastItem is TimelineItem.Thread &&
                lastItem.posts.first().uri != thread.rootPostUri -> list += TimelineItem.Thread(
                id = thread.entity.uri.uri,
                generation = thread.generation,
                anchorPostIndex = 0,
                hasBreak = false,
                posts = listOf(post),
                appliedLabels = appliedLabels,
                signedInProfileId = signedInProfileId,
                postUrisToThreadGates = mapOf(post.uri to threadGate(post.uri)),
            )
            // Just tack the post to the current thread
            lastItem is TimelineItem.Thread ->
                // Make sure only consecutive generations are added to the thread.
                // Nonconsecutive generations are dropped. Users can see these replies by
                // diving into the thread.
                if (lastItem.nextGeneration == thread.generation) list[list.lastIndex] = lastItem.copy(
                    posts = lastItem.posts + post,
                    postUrisToThreadGates = lastItem.postUrisToThreadGates + (post.uri to threadGate(post.uri)),
                )
            else -> Unit
        }
    }
}

private val TimelineItem.Thread.nextGeneration
    get() = generation?.let { it + posts.size }

private fun TimelinePreferencesEntity?.preferredPresentation(): Timeline.Presentation =
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
