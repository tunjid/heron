package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TimelineTitle(
    timeline: Timeline?,
    creator: Profile?
) {
    if (timeline != null) Row(
        modifier = Modifier
            .padding(
                horizontal = 8.dp
            )
    ) {
        timeline.avatarImageArgs?.let { args ->
            AsyncImage(
                modifier = Modifier
                    .size(36.dp),
                args = args,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Text(
                text = timeline.name,
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
            Text(
                text = timeline.getDescription(creator),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private val Timeline.avatarImageArgs: ImageArgs?
    get() = when (this) {
        is Timeline.Home.Feed ->
            if (feedGenerator.avatar == null) null
            else ImageArgs(
                url = feedGenerator.avatar?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                shape = TimelineAvatarShape,
            )

        is Timeline.Home.List ->
            if (feedList.avatar == null) null
            else ImageArgs(
                url = feedList.avatar?.uri,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                shape = TimelineAvatarShape,
            )

        is Timeline.Home.Following,
        is Timeline.Profile,
            -> null
    }

private fun Timeline.getDescription(
    creator: Profile?
): String = when (this) {
    is Timeline.Home.Feed -> creator?.displayName ?: feedGenerator.creatorId.id
    is Timeline.Home.List -> creator?.displayName ?: feedList.creatorId.id

    is Timeline.Home.Following,
    is Timeline.Profile,
        -> null
} ?: ""

private val TimelineAvatarShape = RoundedCornerShape(4.dp).toRoundedPolygonShape()