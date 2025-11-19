package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.shareUri
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.asClipEntry
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.copy_link_to_clipboard
import heron.ui.timeline.generated.resources.send_via_direct_message
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class PostOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentPost: Post? by mutableStateOf(null)

    internal val isSignedIn
        get() = signedInProfileId != null

    override fun onHidden() {
        currentPost = null
    }

    fun showOptions(post: Post) {
        currentPost = post
        show()
    }

    companion object {
        @Composable
        fun rememberUpdatedPostOptionsState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (Post, Conversation) -> Unit,
        ): PostOptionsSheetState {
            val state = rememberBottomSheetState {
                PostOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    scope = it,
                )
            }.also {
                it.signedInProfileId = signedInProfileId
                it.recentConversations = recentConversations
            }

            PostOptionsBottomSheet(
                state = state,
                onShareInConversationClicked = onShareInConversationClicked,
            )

            return state
        }
    }
}

@Composable
private fun PostOptionsBottomSheet(
    state: PostOptionsSheetState,
    onShareInConversationClicked: (Post, Conversation) -> Unit,
) {
    val signedInProfileId = state.signedInProfileId
    if (signedInProfileId != null) state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SendDirectMessageCard(
                signedInProfileId = signedInProfileId,
                recentConversations = state.recentConversations,
                onConversationClicked = { conversation ->
                    val currentPost = state.currentPost
                    if (currentPost != null) {
                        onShareInConversationClicked(currentPost, conversation)
                    }
                    state.hide()
                },
            )
            state.currentPost?.let {
                CopyToClipboardCard(it.uri.shareUri())
            }
        }
    }
}

@Composable
private fun SendDirectMessageCard(
    signedInProfileId: ProfileId,
    recentConversations: List<Conversation>,
    onConversationClicked: (Conversation) -> Unit,
) {
    ShareActionCard(
        showDivider = false,
        topContent = {
            Text(
                modifier = Modifier
                    .padding(
                        vertical = 4.dp,
                    ),
                text = stringResource(Res.string.send_via_direct_message),
                style = MaterialTheme.typography.bodySmall,
            )
            LazyRow(
                modifier = Modifier
                    .clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = recentConversations,
                    key = { it.id.id },
                ) { conversation ->
                    val member = conversation.members.firstOrNull {
                        it.did != signedInProfileId
                    } ?: return@items
                    AsyncImage(
                        args = remember(member.avatar?.uri) {
                            ImageArgs(
                                url = member.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
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
        },
    )
}

@Composable
private fun CopyToClipboardCard(
    uri: GenericUri,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyToClipboardDescription = stringResource(Res.string.copy_link_to_clipboard)

    ShareActionCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            scope.launch {
                clipboard.setClipEntry(uri.asClipEntry(copyToClipboardDescription))
            }
        },
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = copyToClipboardDescription
                }
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = copyToClipboardDescription,
                style = MaterialTheme.typography.bodyLarge,
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShareActionCard(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: @Composable ColumnScope.() -> Unit,
) {
    ShareActionCard(
        modifier = modifier,
        onClick = null,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
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
        },
    )
}

@Composable
private fun ShareActionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    if (onClick == null) ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
    else ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}
