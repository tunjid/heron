package com.tunjid.heron.atmosphereapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.heron.atmosphereapp.AppScreenStateHolders
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.profile.AppLogoZIndex
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.profile.nameOrHandleOrUnknown
import com.tunjid.heron.timeline.utilities.displayName
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import heron.feature.atmosphereapp.generated.resources.Res
import heron.feature.atmosphereapp.generated.resources.profiles_apps
import heron.feature.atmosphereapp.generated.resources.tab_albums
import heron.feature.atmosphereapp.generated.resources.tab_artists
import heron.feature.atmosphereapp.generated.resources.tab_documents
import heron.feature.atmosphereapp.generated.resources.tab_publications
import heron.feature.atmosphereapp.generated.resources.tab_scrobbles
import heron.feature.atmosphereapp.generated.resources.tab_tracks
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun AtmosphereAppHeader(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    headerState: CollapsingHeaderState,
    pagerState: PagerState,
    avatarSharedElementKey: String,
    profile: Profile?,
    app: AtmosphereApp?,
    stateHolders: List<AppScreenStateHolders>,
) {
    Column(
        modifier = modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = -headerState.translation.roundToInt(),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .height(AvatarSize * 1.5f)
                .offset {
                    IntOffset(
                        x = (AvatarOffset * headerState.progress).roundToPx(),
                        y = 0,
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OverlappingAvatars(
                modifier = Modifier
                    .align(
                        BiasAlignment.Vertical(
                            lerp(
                                start = -1f,
                                stop = 0f,
                                fraction = headerState.progress,
                            ),
                        ),
                    )
                    .align(Alignment.Top),
                paneScaffoldState = paneScaffoldState,
                headerState = headerState,
                avatarSharedElementKey = avatarSharedElementKey,
                app = app,
                profile = profile,
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .offset {
                        IntOffset(
                            headerState.horizontalOffset.roundToPx(),
                            0,
                        )
                    }
                    .graphicsLayer {
                        alpha = 1f - headerState.progress
                    },
                text = stringResource(
                    Res.string.profiles_apps,
                    profile.nameOrHandleOrUnknown,
                    app.displayName(),
                ),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight(
                        lerp(
                            start = FontWeight.Bold.weight,
                            stop = FontWeight.Normal.weight,
                            fraction = headerState.progress,
                        ),
                    ),
                ),
            )
        }
        AtmosphereAppTabs(
            modifier = Modifier
                .fillMaxWidth(),
            tabs = stateHolders.map { holder ->
                Tab(
                    title = holder.tabTitle(),
                    id = holder.key,
                    hasUpdate = false,
                )
            },
            pagerState = pagerState,
        )
    }
}

@Composable
fun OverlappingAvatars(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    headerState: CollapsingHeaderState,
    avatarSharedElementKey: String,
    app: AtmosphereApp?,
    profile: Profile?,
) = with(paneScaffoldState) {
    val appDisplayName = app.displayName()

    Row(
        modifier = modifier,
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .zIndex(1f)
                .size(AvatarSize)
                .clip(CircleShape),
            sharedContentState = rememberSharedContentState(
                key = avatarSharedElementKey,
            ),
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(profile?.avatar) {
                    ImageArgs(
                        url = profile?.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile?.displayName ?: profile?.handle?.id,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
        }

        if (app != null) PaneStickySharedElement(
            modifier = Modifier
                .zIndex(1f)
                .size(AvatarSize)
                .offset {
                    IntOffset(
                        headerState.horizontalOffset.roundToPx(),
                        headerState.verticalOffset.roundToPx(),
                    )
                }
                .clip(CircleShape),
            sharedContentState = rememberSharedContentState(app.id),
            zIndexInOverlay = AppLogoZIndex,
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(
                    app.id,
                    appDisplayName,
                ) {
                    ImageArgs(
                        url = app.logo.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = appDisplayName,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )
        }
    }
}

@Composable
private fun AtmosphereAppTabs(
    modifier: Modifier = Modifier,
    tabs: List<Tab>,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    Tabs(
        modifier = modifier.clip(CircleShape),
        tabsState = rememberTabsState(
            tabs = tabs,
            selectedTabIndex = pagerState::tabIndex,
            onTabSelected = {
                scope.launch { pagerState.animateScrollToPage(it) }
            },
            onTabReselected = {},
        ),
    )
}

private val CollapsingHeaderState.horizontalOffset: Dp
    get() = lerp(
        start = -AvatarSize / 2,
        stop = 0.dp,
        fraction = progress,
    )

private val CollapsingHeaderState.verticalOffset: Dp
    get() = lerp(
        start = AvatarSize / 2,
        stop = 0.dp,
        fraction = progress,
    )

@Composable
private fun AppScreenStateHolders.tabTitle(): String = stringResource(
    when (this) {
        is AppScreenStateHolders.StandardSite.Documents -> Res.string.tab_documents
        is AppScreenStateHolders.StandardSite.Publications -> Res.string.tab_publications
        is AppScreenStateHolders.Rocksky.Albums -> Res.string.tab_albums
        is AppScreenStateHolders.Rocksky.Tracks -> Res.string.tab_tracks
        is AppScreenStateHolders.Rocksky.Artists -> Res.string.tab_artists
        is AppScreenStateHolders.Rocksky.Scrobbles -> Res.string.tab_scrobbles
    },
)

private val AvatarSize = 44.dp
private val AvatarOffset = 48.dp

