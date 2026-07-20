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

package com.tunjid.heron.data.utilities.draft

import app.bsky.draft.CreateDraftRequest
import app.bsky.draft.DeleteDraftRequest
import app.bsky.draft.DraftWithId
import app.bsky.draft.GetDraftsQueryParams
import app.bsky.draft.GetDraftsResponse
import app.bsky.draft.UpdateDraftRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.emptyCursorList
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.data.core.types.ExpiredSessionException
import com.tunjid.heron.data.core.types.isNotFound
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.PostDraftDao
import com.tunjid.heron.data.database.entities.PostDraftEntity
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.asEntity
import com.tunjid.heron.data.network.models.asExternalModel
import com.tunjid.heron.data.network.models.toNetworkDraft
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionOutcome
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sh.christian.ozone.api.Tid

/**
 * Reads and writes the signed in user's post drafts, backed by the private stash `app.bsky.draft.*`
 * lexicon and mirrored to a local Room cache for offline listing. Injected into
 * [com.tunjid.heron.data.repository.PostRepository].
 */
internal interface PostDraftDataSource {

    fun drafts(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post.Draft>>

    /**
     * Creates the draft when [Post.Draft.id] is `null`, otherwise updates the existing draft.
     * Returns the server assigned [DraftId] so callers can switch a first save into subsequent
     * in-place updates.
     */
    suspend fun saveDraft(
        draft: Post.Draft,
    ): Result<DraftId>

    suspend fun deleteDraft(
        id: DraftId,
    ): Outcome
}

@Inject
internal class OfflinePostDraftDataSource(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val postDraftDao: PostDraftDao,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : PostDraftDataSource {

    override fun drafts(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post.Draft>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                postDraftDao.postDrafts(
                    authorId = signedInProfileId,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChanged()
                    .map { entities ->
                        entities.map(PostDraftEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getDrafts(
                            GetDraftsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetDraftsResponse::cursor,
                    onResponse = {
                        postDraftDao.upsertPostDrafts(
                            drafts.map { it.asEntity(signedInProfileId) },
                        )
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .map { it ?: emptyCursorList() }
            .flowOn(ioDispatcher)

    override suspend fun saveDraft(
        draft: Post.Draft,
    ): Result<DraftId> = savedStateDataSource.inCurrentProfileSession currentSession@{ signedInProfileId ->
        if (signedInProfileId == null) return@currentSession Result.failure(ExpiredSessionException())

        val networkDraft = draft.toNetworkDraft()
        val result = when (val existingId = draft.id) {
            null -> networkService.runCatchingWithMonitoredNetworkRetry {
                createDraft(
                    request = CreateDraftRequest(draft = networkDraft),
                )
            }.map { response ->
                response.id.let(::DraftId)
            }

            else -> networkService.runCatchingWithMonitoredNetworkRetry {
                updateDraft(
                    UpdateDraftRequest(
                        draft = DraftWithId(
                            id = existingId.id.let(::Tid),
                            draft = networkDraft,
                        ),
                    ),
                )
            }.map { existingId }
        }

        result.onSuccess { draftId ->
            runCatchingUnlessCancelled {
                postDraftDao.upsertPostDrafts(
                    listOf(
                        draft.copy(id = draftId).asEntity(fallback = Clock.System.now()),
                    ),
                )
            }
        }
    } ?: Result.failure(ExpiredSessionException())

    override suspend fun deleteDraft(
        id: DraftId,
    ): Outcome = savedStateDataSource.inCurrentProfileSession currentSession@{ signedInProfileId ->
        if (signedInProfileId == null) return@currentSession expiredSessionOutcome()

        networkService.runCatchingWithMonitoredNetworkRetry {
            deleteDraft(
                DeleteDraftRequest(id = id.id.let(::Tid)),
            )
        }
            .recoverCatching {
                if (it.isNotFound()) Unit
                else throw it
            }
            .toOutcome {
                postDraftDao.deletePostDraft(id)
            }
    } ?: expiredSessionOutcome()
}
