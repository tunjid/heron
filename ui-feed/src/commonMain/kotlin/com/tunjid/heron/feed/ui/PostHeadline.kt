package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import kotlinx.datetime.Instant

@Composable
internal fun PostHeadline(
    now: Instant,
    createdAt: Instant,
    author: Profile,
) {
    Column {
        val primaryText = author.displayName ?: author.handle.id
        val secondaryText = author.handle.id.takeUnless { it == primaryText }

        Row(horizontalArrangement = spacedBy(4.dp)) {
            Text(
                modifier = Modifier.weight(1f),
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
            Text(
                modifier = Modifier,
                text = author.handle.id,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
            )

        }
    }
}
