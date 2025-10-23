package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.copy_link_icon
import heron.ui.timeline.generated.resources.copy_link_to_post
import heron.ui.timeline.generated.resources.send_icon
import heron.ui.timeline.generated.resources.send_via_direct_message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class PostOptionsSheetState private constructor(
    isSignedIn: Boolean,
    recentConversations: List<Conversation>,
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {

    var showBottomSheet by mutableStateOf(false)
        internal set

    internal var isSignedIn by mutableStateOf(isSignedIn)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentPost: Post? by mutableStateOf(null)

    internal fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentPost = null
            }
        }
    }

    fun showOptions(post: Post?) {
        currentPost = post
        showBottomSheet = true
    }

    companion object {
        @Composable
        fun rememberUpdatedPostOptionsState(
            isSignedIn: Boolean,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (Post, Conversation) -> Unit,
        ): PostOptionsSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            val state = remember(sheetState, scope) {
                PostOptionsSheetState(
                    isSignedIn = isSignedIn,
                    recentConversations = recentConversations,
                    sheetState = sheetState,
                    scope = scope,
                )
            }.also {
                it.isSignedIn = isSignedIn
                it.recentConversations = recentConversations
            }

            PostOptionsBottomSheet(
                state = state,
                onShareInConversationClicked = onShareInConversationClicked,
                recentConversations = recentConversations,
            )

            return state
        }
    }
}

@Composable
private fun PostOptionsBottomSheet(
    state: PostOptionsSheetState,
    recentConversations: List<Conversation>,
    onShareInConversationClicked: (Post, Conversation) -> Unit,
) {
    if (state.showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                state.showBottomSheet = false
            },
            sheetState = state.sheetState,
            content = {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SendDirectMessageCard(
                        conversations = recentConversations,
                        onSendClicked = {
                            // TODO implement send direct message
                        },
                        onConversationClicked = { conversation ->
                            state.scope.launch {
                                state.hideSheet()
                                val currentPost = state.currentPost
                                if (currentPost != null) {
                                    onShareInConversationClicked(currentPost, conversation)
                                }
                            }
                        },
                    )
//                    CopyLinkCard(
//                        onCopyLinkClicked = {
//                        },
//                    )
                }
            },
        )
    }
}

@Composable
private fun SendDirectMessageCard(
    conversations: List<Conversation>,
    onSendClicked: () -> Unit,
    onConversationClicked: (Conversation) -> Unit,
) {
    ShareActionCard(
        showDivider = true,
        topContent = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(conversations) { conversation ->
                    val member = conversation.members.firstOrNull() ?: return@items
                    AsyncImage(
                        args = ImageArgs(
                            url = member.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            shape = RoundedPolygonShape.Circle,
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                onConversationClicked(conversation)
                            },
                    )
                }
            }
        },
        bottomContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSendClicked() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.send_via_direct_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(Res.string.send_icon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun CopyLinkCard(
    onCopyLinkClicked: () -> Unit,
) {
    ShareActionCard(
        bottomContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyLinkClicked() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.copy_link_to_post),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(Res.string.copy_link_icon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun ShareActionCard(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            topContent?.invoke(this)

            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 0.6.dp,
                )
            }
            bottomContent()
        }
    }
}
