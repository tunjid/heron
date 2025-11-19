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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.ui.MediaUploadItems
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.utilities.EmbeddedRecord
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.AvatarSize
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.detectActiveLink
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.formatTextPost
import com.tunjid.heron.ui.text.insertMention
import com.tunjid.heron.ui.text.links
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.remove_quoted_post
import heron.feature.compose.generated.resources.remove_shared_record
import heron.ui.core.generated.resources.record_feed
import heron.ui.core.generated.resources.record_labeler
import heron.ui.core.generated.resources.record_list
import heron.ui.core.generated.resources.record_starter_pack
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ComposeScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        val postText = state.postText
        ReplyingTo(
            paneMovableElementSharedTransitionScope = paneScaffoldState,
            type = state.postType,
            sharedElementPrefix = state.sharedElementPrefix,
        )
        Post(
            signedInProfile = state.signedInProfile,
            postText = postText,
            embeddedRecord = state.embeddedRecord,
            paneMovableElementSharedTransitionScope = paneScaffoldState,
            onPostTextChanged = { actions(Action.PostTextChanged(it)) },
            onMentionDetected = {
                actions(Action.SearchProfiles(it))
            },
            onRemoveEmbeddedRecordClicked = {
                actions(Action.RemoveEmbeddedRecord)
            },
        )
        if (state.suggestedProfiles.isNotEmpty()) {
            AutoCompletePostProfileSearchResults(
                results = state.suggestedProfiles.take(MAX_SUGGESTED_PROFILES),
                onProfileClicked = { profile ->
                    // insert handle into text field
                    actions(
                        Action.PostTextChanged(
                            insertMention(state.postText, profile.handle.id),
                        ),
                    )
                    actions(Action.ClearSuggestions)
                },
            )
        }
        MediaUploadItems(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            photos = state.photos,
            video = state.video,
            removeMediaItem = { item ->
                actions(Action.EditMedia.RemoveMedia(item))
            },
            onMediaItemUpdated = { item ->
                actions(Action.EditMedia.UpdateMedia(item))
            },
        )
        Spacer(
            modifier = Modifier
                .height(56.dp),
        )

        LaunchedEffect(Unit) {
            snapshotFlow { state.postText.text.length }
                .collect {
                    scrollState.scrollTo(Int.MAX_VALUE)
                }
        }
    }
}

@Composable
private fun Post(
    modifier: Modifier = Modifier,
    signedInProfile: Profile?,
    postText: TextFieldValue,
    embeddedRecord: Record?,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onPostTextChanged: (TextFieldValue) -> Unit,
    onMentionDetected: (String) -> Unit,
    onRemoveEmbeddedRecordClicked: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AuthorAndPost(
            modifier = modifier,
            avatar = {
                AsyncImage(
                    modifier = Modifier.size(UiTokens.avatarSize),
                    args = remember(signedInProfile?.avatar) {
                        ImageArgs(
                            url = signedInProfile?.avatar?.uri,
                            contentDescription = signedInProfile?.contentDescription,
                            contentScale = ContentScale.Crop,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                )
            },
            postContent = {
                PostComposition(
                    modifier = Modifier
                        .fillMaxWidth(),
                    postText = postText,
                    onPostTextChanged = onPostTextChanged,
                    onMentionDetected = onMentionDetected,
                )
            },
        )

        embeddedRecord?.let {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                EmbeddedRecord(
                    modifier = Modifier
                        .weight(1f),
                    record = it,
                    sharedElementPrefix = NeverMatchedSharedElementPrefix,
                    movableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    postActions = PostActions.NoOp,
                )
                val contentDescription = when (it) {
                    is Labeler -> stringResource(
                        Res.string.remove_shared_record,
                        stringResource(CommonStrings.record_labeler),
                    )
                    is Post -> stringResource(Res.string.remove_quoted_post)
                    is FeedGenerator -> stringResource(
                        Res.string.remove_shared_record,
                        stringResource(CommonStrings.record_feed),
                    )
                    is FeedList -> stringResource(
                        Res.string.remove_shared_record,
                        stringResource(CommonStrings.record_list),
                    )
                    is StarterPack -> stringResource(
                        Res.string.remove_shared_record,
                        stringResource(CommonStrings.record_starter_pack),
                    )
                }
                FilledTonalIconButton(
                    onClick = onRemoveEmbeddedRecordClicked,
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = contentDescription,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReplyingTo(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    type: Post.Create?,
    sharedElementPrefix: String?,
) {
    when (type) {
        is Post.Create.Mention -> Unit
        is Post.Create.Reply -> AuthorAndPost(
            modifier = modifier,
            avatar = {
                paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
                    modifier = Modifier
                        .size(AvatarSize),
                    sharedContentState = with(paneMovableElementSharedTransitionScope) {
                        rememberSharedContentState(
                            key = type.parent.avatarSharedElementKey(sharedElementPrefix),
                        )
                    },
                    state = remember(type.parent.author.avatar) {
                        ImageArgs(
                            url = type.parent.author.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = type.parent.author.displayName
                                ?: type.parent.author.handle.id,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                    sharedElement = { state, modifier ->
                        AsyncImage(state, modifier)
                    },
                )
            },
            postContent = {
                Column {
                    ProfileName(
                        profile = type.parent.author,
                        ellipsize = true,
                    )
                    Text(text = type.parent.record?.text ?: "")
                }
            },
        )

        Post.Create.Timeline -> Unit
        else -> Unit
    }
}

@Composable
private inline fun AuthorAndPost(
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
    postContent: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        avatar()
        postContent()
    }
}

@Composable
private fun PostComposition(
    modifier: Modifier,
    postText: TextFieldValue,
    onPostTextChanged: (TextFieldValue) -> Unit,
    onMentionDetected: (String) -> Unit,
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    BasicTextField(
        modifier = modifier
            .focusRequester(textFieldFocusRequester)
            .bringIntoViewRequester(bringIntoViewRequester),
        value = postText,
        onValueChange = {
            val links = it.annotatedString.links()
            val annotated = formatTextPost(
                text = it.text,
                textLinks = links,
            )
            onPostTextChanged(
                it.copy(annotatedString = annotated),
            )
            when (val target = links.detectActiveLink(it.selection)) {
                is LinkTarget.UserHandleMention -> onMentionDetected(target.handle.id)
                is LinkTarget.Hashtag -> {
                    // TODO: Implement hashtag search
                }
                else -> Unit
            }
        },
        onTextLayout = {
            val cursorRect = it.getCursorRect(postText.selection.start)
            coroutineScope.launch {
                bringIntoViewRequester.bringIntoView(cursorRect)
            }
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current,
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Default,
        ),
    )

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }
}

@Composable
fun AutoCompletePostProfileSearchResults(
    modifier: Modifier = Modifier,
    results: List<Profile>,
    onProfileClicked: (Profile) -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column {
            results.forEachIndexed { index, profile ->
                ProfileResultItem(
                    profile = profile,
                    onProfileClicked = onProfileClicked,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )

                if (index != results.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.8.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileResultItem(
    profile: Profile,
    onProfileClicked: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    AttributionLayout(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProfileClicked(profile) },
        avatar = {
            AsyncImage(
                args = remember(profile.avatar) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                modifier = Modifier
                    .size(36.dp),
            )
        },
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ProfileName(profile = profile)
                ProfileHandle(profile = profile)
            }
        },
        action = { /* no follow/edit chip for mention autocomplete */ },
    )
}

@OptIn(ExperimentalUuidApi::class)
private val NeverMatchedSharedElementPrefix = Uuid.random().toString()
