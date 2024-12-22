package com.tunjid.heron.timeline.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostHeadline(
    now: Instant,
    createdAt: Instant,
    author: Profile,
    postId: Id,
    sharedElementPrefix: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
) = with(sharedTransitionScope) {
    Column {
        val primaryText = author.displayName ?: author.handle.id
        val secondaryText = author.handle.id.takeUnless { it == primaryText }

        Row(horizontalArrangement = spacedBy(4.dp)) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        state = rememberSharedContentState(
                            author.textSharedElementKey(
                                prefix = sharedElementPrefix,
                                postId = postId,
                                text = primaryText
                            )
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                text = primaryText,
                maxLines = 1,
                style = LocalTextStyle.current.copy(fontWeight = Bold),
            )

            TimeDelta(
                modifier = Modifier.alignByBaseline(),
                delta = now - createdAt,
            )
        }
        if (secondaryText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(
                            author.textSharedElementKey(
                                prefix = sharedElementPrefix,
                                postId = postId,
                                text = secondaryText
                            )
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                text = author.handle.id,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
            )

        }
    }
}

private fun Profile.textSharedElementKey(
    prefix: String,
    postId: Id,
    text: String,
): String = "$prefix-${postId.id}-${did.id}-$text"