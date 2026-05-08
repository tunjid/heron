package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
fun ProfileApps(
    modifier: Modifier = Modifier,
    apps: List<AtmosphereApp>,
    onAppClicked: (AtmosphereApp) -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = apps,
            key = AtmosphereApp::id,
            itemContent = { app ->
                ProfileApp(
                    app = app,
                    onAppClicked = onAppClicked,
                )
            },
        )
    }
}

@Composable
private fun ProfileApp(
    modifier: Modifier = Modifier,
    app: AtmosphereApp,
    onAppClicked: (AtmosphereApp) -> Unit,
) {
    ElevatedAssistChip(
        modifier = modifier,
        shape = CircleShape,
        leadingIcon = {
            AsyncImage(
                modifier = Modifier
                    .size(24.dp),
                args = remember(app.logo) {
                    ImageArgs(
                        url = app.logo.uri,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
        },
        label = {
            Text(app.id)
        },
        onClick = {
            onAppClicked(app)
        },
    )
}
