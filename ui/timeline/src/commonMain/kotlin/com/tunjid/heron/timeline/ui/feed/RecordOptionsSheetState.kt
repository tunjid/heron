package com.tunjid.heron.timeline.ui.feed

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButtonDefaults.Icon
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.asClipEntry
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.copy_link_to_clipboard
import heron.ui.timeline.generated.resources.send_via_direct_message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class RecordOptionsSheetState private constructor(
    signedInProfileId: ProfileId?,
    recentConversations: List<Conversation>,
    internal val sheetState: SheetState,
    internal val scope: CoroutineScope,
) {

    var showBottomSheet by mutableStateOf(false)
        internal set

    internal var signedInProfileId by mutableStateOf(signedInProfileId)

    internal var recentConversations by mutableStateOf(recentConversations)

    internal var currentRecordUri: RecordUri? by mutableStateOf(null)

    internal val isSignedIn get() = signedInProfileId != null

    internal fun hideSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                currentRecordUri = null
            }
        }
    }

    fun showOptions(recordUri: RecordUri) {
        currentRecordUri = recordUri
        showBottomSheet = true
    }

    companion object {
        @Composable
        fun rememberUpdatedRecordOptionsState(
            signedInProfileId: ProfileId?,
            recentConversations: List<Conversation>,
            onShareInConversationClicked: (RecordUri, Conversation) -> Unit,
        ): RecordOptionsSheetState {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            val state = remember(sheetState, scope) {
                RecordOptionsSheetState(
                    signedInProfileId = signedInProfileId,
                    recentConversations = recentConversations,
                    sheetState = sheetState,
                    scope = scope,
                )
            }.also {
                it.signedInProfileId = signedInProfileId
                it.recentConversations = recentConversations
            }

            RecordOptionsBottomSheet(
                state = state,
                onShareInConversationClicked = onShareInConversationClicked,
            )

            return state
        }
    }
}

@Composable
private fun RecordOptionsBottomSheet(
    state: RecordOptionsSheetState,
    onShareInConversationClicked: (RecordUri, Conversation) -> Unit,
) {
    val signedInProfileId = state.signedInProfileId
    if (state.showBottomSheet && signedInProfileId != null) {
        ModalBottomSheet(
            onDismissRequest = { state.showBottomSheet = false },
            sheetState = state.sheetState,
            content = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SendDirectMessageCard(
                        signedInProfileId = signedInProfileId,
                        recentConversations = state.recentConversations,
                        onConversationClicked = { conversation ->
                            state.scope.launch {
                                state.hideSheet()
                                state.currentRecordUri?.let { uri ->
                                    onShareInConversationClicked(uri, conversation)
                                }
                            }
                        },
                    )

                    state.currentRecordUri?.let { uri ->
                        CopyToClipboardCard(uri.shareUri())
                    }
                }
            },
        )
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

fun RecordUri.shareUri(): GenericUri {
    if (!uri.startsWith("at://")) return GenericUri(uri)

    val parts = uri.substringAfter("at://").split('/')
    if (parts.size < 3) return GenericUri(uri)

    val did = parts[0]
    val rkey = parts[2]

    return GenericUri(
        when (this) {
            is FeedGeneratorUri -> "https://bsky.app/profile/$did/feed/$rkey"
            is ListUri -> "https://bsky.app/profile/$did/list/$rkey"
            // The rkey of a starter pack is the at-uri of the list it contains
            is StarterPackUri -> "https://bsky.app/starter-pack/$rkey"
            // The rkey for a labeler is 'self'
            is LabelerUri -> "https://bsky.app/profile/$did"
            else -> uri
        },
    )
}
