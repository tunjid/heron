package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.toRoundedPolygonShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Timeline(
    timeline: Timeline?,
) {
    if (timeline != null) Row {
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
                text = timeline.description,
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

private val Timeline.description: String
    get() = when (this) {
        is Timeline.Home.Feed -> feedGenerator.creatorId.id
        is Timeline.Home.List -> feedList.creatorId.id

        is Timeline.Home.Following,
        is Timeline.Profile,
            -> null
    } ?: ""

private val TimelineAvatarShape = RoundedCornerShape(4.dp).toRoundedPolygonShape()