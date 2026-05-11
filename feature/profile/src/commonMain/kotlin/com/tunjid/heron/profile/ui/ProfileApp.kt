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
import com.tunjid.heron.profile.AppLogoZIndex
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.utilities.displayName
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
fun ProfileApps(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
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
                    paneScaffoldState = paneScaffoldState,
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
    paneScaffoldState: PaneScaffoldState,
    app: AtmosphereApp,
    onAppClicked: (AtmosphereApp) -> Unit,
) = with(paneScaffoldState) {
    ElevatedAssistChip(
        modifier = modifier,
        shape = CircleShape,
        leadingIcon = {
            PaneStickySharedElement(
                modifier = Modifier
                    .size(24.dp),
                sharedContentState = rememberSharedContentState(app.id),
                zIndexInOverlay = AppLogoZIndex,
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap(),
                    args = remember(app.logo) {
                        ImageArgs(
                            url = app.logo.uri,
                            contentScale = ContentScale.Crop,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                )
            }
        },
        label = {
            Text(app.displayName())
        },
        onClick = {
            onAppClicked(app)
        },
    )
}
