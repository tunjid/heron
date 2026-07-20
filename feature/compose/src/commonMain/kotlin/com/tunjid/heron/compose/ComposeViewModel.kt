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

package com.tunjid.heron.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.model
import com.tunjid.heron.ui.scaffold.navigation.sharedUri
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.links
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.saving_draft
import heron.feature.compose.generated.resources.sending_post
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

@Stable
internal interface ComposeStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ComposeViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel
}

@Stable
class ActualComposeViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ComposeStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        authRepository: AuthRepository,
        searchRepository: SearchRepository,
        userDataRepository: UserDataRepository,
        recordRepository: RecordRepository,
        fileManager: FileManager,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchLoadSignedInProfileMutations(
                    state = state,
                    authRepository = authRepository,
                )
                launchInteractionSettingsMutations(
                    state = state,
                    userDataRepository = userDataRepository,
                )
                launchEmbeddedRecordMutations(
                    state = state,
                    embeddedRecordUri = when (val creationType = route.model<Post.Create.Quote>()) {
                        is Post.Create.Quote -> creationType.interaction.postUri
                        else -> route.sharedUri?.asEmbeddableRecordUriOrNull()
                    },
                    recordRepository = recordRepository,
                )

                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.PostTextChanged -> action.flow.launchPostTextMutations(
                            state = state,
                        )
                        is Action.SetFabExpanded -> action.flow.launchFabExpansionMutations(
                            state = state,
                        )
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                            state = state,
                        )
                        is Action.UpdateInteractionSettings -> action.flow.launchUpdateInteractionSettingsMutations(
                            state = state,
                        )
                        is Action.EditMedia -> action.flow.launchEditMediaMutations(
                            state = state,
                        )
                        is Action.CreatePost -> action.flow.launchCreatePostMutations(
                            state = state,
                            navActions = navActions,
                            writeQueue = writeQueue,
                            fileManager = fileManager,
                        )
                        is Action.LoadDraft -> action.flow.launchLoadDraftMutations(
                            state = state,
                            fileManager = fileManager,
                        )
                        is Action.SaveDraft -> action.flow.launchSaveDraftMutations(
                            state = state,
                            navActions = navActions,
                            writeQueue = writeQueue,
                            fileManager = fileManager,
                        )
                        is Action.SearchProfiles -> action.flow.launchSearchMutations(
                            state = state,
                            searchRepository = searchRepository,
                        )
                        is Action.ClearSuggestions -> action.flow.launchClearSuggestionsMutations(
                            state = state,
                        )
                        is Action.RemoveDetectedUri -> action.flow.launchRemoveDetectedUriMutations(
                            state = state,
                        )
                        is Action.UriDetected -> action.flow.launchEmbedUrlMutations(
                            state = state,
                            recordRepository = recordRepository,
                        )
                        is Action.Navigate -> action.flow.collect { navAction ->
                            navActions(navAction.navigationMutation)
                        }
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun launchInteractionSettingsMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.interactionsPreference = it.postInteractionSettings
}

context(productionScope: CoroutineScope)
private fun launchEmbeddedRecordMutations(
    state: State.SnapshotMutable,
    embeddedRecordUri: EmbeddableRecordUri?,
    recordRepository: RecordRepository,
) {
    embeddedRecordUri?.let { uri ->
        recordRepository.embeddableRecord(uri).launchedCollect {
            state.embeddedRecord = it as? Record.Embeddable.Native
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UriDetected>.launchEmbedUrlMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = debounce(400.milliseconds)
    .launchedCollectLatest { action ->
        val trimmedUrl = action.url.trimEnd('/', '\n', '\r', ' ')
        when (val uri = trimmedUrl.asEmbeddableRecordUriOrNull()) {
            null -> try {
                // Not an embeddable AT-record; resolve the URL to an external link card preview.
                state.isLoadingLinkPreview = true
                state.linkPreview = recordRepository.externalLinkPreview(GenericUri(action.url))
            } finally {
                state.isLoadingLinkPreview = false
            }
            else -> {
                state.embeddedRecord = recordRepository.embeddableRecord(uri)
                    .firstOrNull() as? Record.Embeddable.Native
            }
        }
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.PostTextChanged>.launchPostTextMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.postText = action.textFieldValue
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetFabExpanded>.launchFabExpansionMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.fabExpanded = action.expanded
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.messages -= action.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.RemoveDetectedUri>.launchRemoveDetectedUriMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    when (action.uri) {
        state.embeddedRecord?.embeddableRecordUri -> state.embeddedRecord = null
        state.linkPreview?.embed?.uri -> state.linkPreview = null
        else -> Unit
    }
    state.dismissedUri = action.uri
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateInteractionSettings>.launchUpdateInteractionSettingsMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.interactionsPreference = action.interactionSettingsPreference
}

context(productionScope: CoroutineScope)
private fun Flow<Action.EditMedia>.launchEditMediaMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    // Invoke in IO context as creating media items may perform IO
    val media = withContext(Dispatchers.IO) {
        when (action) {
            is Action.EditMedia.AddPhotos -> action.photos
            is Action.EditMedia.AddVideo -> listOfNotNull(action.video)
            is Action.EditMedia.RemoveMedia -> emptyList()
            is Action.EditMedia.UpdateMedia -> listOfNotNull(action.media)
        }
    }
    when (action) {
        is Action.EditMedia.AddPhotos -> {
            state.photos += media.filterIsInstance<RestrictedFile.Media.Photo>()
            state.video = null
        }

        is Action.EditMedia.AddVideo -> {
            state.photos = emptyList()
            state.video = media.filterIsInstance<RestrictedFile.Media.Video>().firstOrNull()
        }

        is Action.EditMedia.RemoveMedia -> {
            state.photos = state.photos.filter { it != action.media }
            state.video = state.video?.takeIf { it != action.media }
        }

        is Action.EditMedia.UpdateMedia -> when (val item = media.firstOrNull()) {
            is RestrictedFile.Media.Photo -> state.photos = state.photos.map { photo ->
                if (photo.path == item.path) item
                else photo
            }

            is RestrictedFile.Media.Video -> state.video = item
            else -> Unit
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.CreatePost>.launchCreatePostMutations(
    state: State.SnapshotMutable,
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
) = launchedCollect { action ->
    val postWrite = withContext(Dispatchers.IO) {
        Writable.Create(
            request = composeRequest(
                authorId = action.authorId,
                text = action.text,
                links = action.links,
                reply = action.postType as? Post.Create.Reply,
                media = action.media,
                embeddedRecordReference = action.embeddedRecordReference,
                linkPreview = action.linkPreview,
                allowed = action.interactionPreference?.threadGateAllowed,
                fileManager = fileManager,
            ),
            sourceDraftId = action.sourceDraftId,
        )
    }

    val status = writeQueue.enqueue(postWrite)
    val memo = postWrite.writeStatusMessage(status)
    if (memo != null) state.messages += memo

    if (status !is WriteQueue.Status.Enqueued) return@launchedCollect

    state.messages += Memo.Resource(stringResource = Res.string.sending_post)

    // Wait for the user to read the message
    delay(1400.milliseconds)

    navActions(Action.Navigate.Pop.navigationMutation)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SaveDraft>.launchSaveDraftMutations(
    state: State.SnapshotMutable,
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
) = launchedCollect {
    val authorId = state.signedInProfile?.did ?: return@launchedCollect
    val draftWrite = withContext(Dispatchers.IO) {
        Writable.PostDraft.Save(
            draft = Post.Draft(
                id = state.draftId,
                authorId = authorId,
                posts = listOf(
                    composeRequest(
                        authorId = authorId,
                        text = state.postText.text,
                        links = state.postText.annotatedString.links(),
                        // Drafts have no reply parent, so a draft is always a top-level post.
                        reply = null,
                        media = state.video?.let(::listOf) ?: state.photos,
                        embeddedRecordReference = state.embeddedRecord?.reference,
                        linkPreview = state.linkPreview,
                        allowed = state.interactionsPreference?.threadGateAllowed,
                        fileManager = fileManager,
                    ),
                ),
            ),
        )
    }

    val status = writeQueue.enqueue(draftWrite)
    val memo = draftWrite.writeStatusMessage(status)
    if (memo != null) state.messages += memo

    if (status !is WriteQueue.Status.Enqueued) return@launchedCollect

    state.messages += Memo.Resource(stringResource = Res.string.saving_draft)

    // Wait for the user to read the message
    delay(1400.milliseconds)

    navActions(Action.Navigate.Pop.navigationMutation)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.LoadDraft>.launchLoadDraftMutations(
    state: State.SnapshotMutable,
    fileManager: FileManager,
) = launchedCollect { action ->
    state.draftId = action.draft.id
    val firstPost = action.draft.posts.firstOrNull()
    val text = firstPost?.text.orEmpty()
    state.photos = emptyList()
    state.video = null
    state.embeddedRecord = null
    state.linkPreview = null
    state.postText = TextFieldValue(
        annotatedString = AnnotatedString(text),
        selection = TextRange(text.length),
    )
    // Media and link previews are best-effort: a draft's media carries no dimensions (so it would
    // be dropped on post) and its link card is not stored, so only text is rehydrated. The link
    // card re-resolves via URL detection once the text is edited.
}

/**
 * Builds the [Post.Create.Request] shared by post creation and draft saving, caching any
 * [RestrictedFile] media into app storage. Photos without a known size are dropped (the post
 * embed requires dimensions); videos are always kept.
 */
private suspend fun composeRequest(
    authorId: ProfileId,
    text: String,
    links: List<Link>,
    reply: Post.Create.Reply?,
    media: List<RestrictedFile.Media>,
    embeddedRecordReference: Record.Reference?,
    linkPreview: LinkPreview?,
    allowed: ThreadGate.Allowed?,
    fileManager: FileManager,
): Post.Create.Request = Post.Create.Request(
    authorId = authorId,
    text = text,
    links = links,
    metadata = Post.Create.Metadata(
        reply = reply,
        embeddedRecordReference = embeddedRecordReference,
        embeddedMedia = media.mapNotNull { item ->
            when (item) {
                is RestrictedFile.Media.Photo ->
                    if (item.hasSize) fileManager.cacheWithoutRestrictions(item)
                    else null

                is RestrictedFile.Media.Video -> fileManager.cacheWithoutRestrictions(item)
            }
        },
        allowed = allowed,
        linkPreview = linkPreview,
    ),
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SearchProfiles>.launchSearchMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = debounce(SEARCH_DEBOUNCE)
    .launchedCollectLatest { action ->
        searchRepository.autoCompleteProfileSearch(
            query = SearchQuery.OfProfiles(
                query = action.query,
                isLocalOnly = false,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                    limit = MAX_SUGGESTED_PROFILES.toLong(),
                ),
            ),
            cursor = Cursor.Initial,
        ).collect { profiles ->
            state.suggestedProfiles = profiles.map(ProfileWithViewerState::profile)
        }
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.ClearSuggestions>.launchClearSuggestionsMutations(
    state: State.SnapshotMutable,
) = launchedCollect {
    state.suggestedProfiles = emptyList()
}

private val SEARCH_DEBOUNCE = 300.milliseconds
const val MAX_SUGGESTED_PROFILES = 5
