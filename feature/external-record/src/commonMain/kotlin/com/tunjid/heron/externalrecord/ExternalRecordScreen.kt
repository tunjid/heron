/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.externalrecord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.externalrecord.ui.Documents
import com.tunjid.heron.externalrecord.ui.EventComments
import com.tunjid.heron.externalrecord.ui.EventHeader
import com.tunjid.heron.externalrecord.ui.RsvpProfiles
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.navigableLinkTargetHandler
import com.tunjid.heron.ui.pinnedHeaderOffset
import com.tunjid.heron.ui.pullToRefreshIndicatorOffset
import com.tunjid.heron.ui.rememberPinnedCollapsingHeaderState
import com.tunjid.heron.ui.rememberSimpleDialogState
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.pathDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.headerBlur
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.heron.ui.scaffold.scaffold.pinnedHeaderScrim
import com.tunjid.heron.ui.scaffold.scaffold.rememberPinnedHeaderState
import com.tunjid.heron.ui.scaffold.scaffold.renderHeaderInTransitionOverlay
import com.tunjid.heron.ui.statusAndToolbarHeight
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.links
import com.tunjid.heron.ui.text.rememberFormattedTextPost
import heron.feature.external_record.generated.resources.Res
import heron.feature.external_record.generated.resources.event_comments
import heron.feature.external_record.generated.resources.event_going
import heron.feature.external_record.generated.resources.event_interested
import heron.feature.external_record.generated.resources.event_not_going
import heron.feature.external_record.generated.resources.publication
import heron.feature.external_record.generated.resources.see_more
import heron.ui.core.generated.resources.close
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExternalRecordScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { state.stateHolders.size }
    val scope = rememberCoroutineScope()
    val hasTabs = state.record is Event || state.stateHolders.size > 1

    val pinnedCollapsingHeaderState = rememberPinnedCollapsingHeaderState(
        pinnedHeight = when {
            hasTabs -> UiTokens.statusAndToolbarHeight + UiTokens.tabsHeight
            else -> UiTokens.statusAndToolbarHeight
        },
        initialExpandedHeight = 800.dp,
    )

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = state.stateHolders.getOrNull(pagerState.currentPage)
        ?.isRefreshing == true

    CollapsingHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .paneClip()
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
                onRefresh = {
                    state.stateHolders.getOrNull(pagerState.currentPage)
                        ?.refresh()
                },
            ),
        state = pinnedCollapsingHeaderState.collapsingHeaderState,
        headerContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .renderHeaderInTransitionOverlay(
                        paneScaffoldState = paneScaffoldState,
                        zIndexInOverlay = UiTokens.lowerThanAppBarSharedElementZIndex(),
                    )
                    .pinnedHeaderScrim(
                        state = pinnedCollapsingHeaderState,
                        scrimColor = MaterialTheme.colorScheme.surface,
                    )
                    .pinnedHeaderOffset(
                        state = pinnedCollapsingHeaderState,
                    ),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiTokens.statusAndToolbarHeight),
                )
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                        )
                        .graphicsLayer {
                            alpha = 1 - pinnedCollapsingHeaderState.progress
                        },
                ) {
                    when (val record = state.record) {
                        is StandardPublication -> PublicationHeader(
                            publication = record,
                            actions = actions,
                        )
                        is Event -> EventHeader(
                            paneScaffoldState = paneScaffoldState,
                            sharedElementPrefix = state.sharedElementPrefix
                                ?: ExternalRecordSharedElementPrefix,
                            event = record,
                            actions = actions,
                        )
                        else -> Unit
                    }
                }
                if (hasTabs) Tabs(
                    modifier = Modifier
                        .padding(
                            horizontal = 8.dp,
                        )
                        .fillMaxWidth()
                        .clip(CircleShape),
                    tabsState = rememberTabsState(
                        tabs = state.stateHolders.map { stateHolder ->
                            Tab(
                                title = stateHolder.tabTitle(),
                                hasUpdate = false,
                            )
                        },
                        selectedTabIndex = pagerState::tabIndex,
                        onTabSelected = { page ->
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        onTabReselected = { page ->
                            state.stateHolders.getOrNull(page)
                                ?.refresh()
                        },
                    ),
                )
            }
            DismissableRefreshIndicator(
                modifier = Modifier
                    .pullToRefreshIndicatorOffset(
                        state = pinnedCollapsingHeaderState,
                    ),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onDismissRequest = {},
            )
        },
        body = {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .headerBlur(
                        state = paneScaffoldState.rememberPinnedHeaderState(
                            pinnedCollapsingHeaderState = pinnedCollapsingHeaderState,
                        ),
                    ),
                state = pagerState,
                key = { page -> state.stateHolders[page].key },
                pageContent = { page ->
                    when (val stateHolder = state.stateHolders[page]) {
                        is ExternalRecordStateHolders.Documents -> Documents(
                            holder = stateHolder,
                            paneScaffoldState = paneScaffoldState,
                            actions = actions,
                        )
                        is ExternalRecordStateHolders.Rsvps -> RsvpProfiles(
                            paneScaffoldState = paneScaffoldState,
                            holder = stateHolder,
                            actions = actions,
                        )
                        is ExternalRecordStateHolders.Comments -> EventComments(
                            paneScaffoldState = paneScaffoldState,
                            holder = stateHolder,
                            showEngagementMetrics = state.preferences.local.showPostEngagementMetrics,
                            actions = actions,
                        )
                    }
                },
            )
        },
    )
}

@Composable
private fun ExternalRecordStateHolders.tabTitle(): String = when (this) {
    is ExternalRecordStateHolders.Documents -> stringResource(Res.string.publication)
    is ExternalRecordStateHolders.Rsvps -> stringResource(
        when (state.status) {
            Rsvp.Status.Interested -> Res.string.event_interested
            Rsvp.Status.NotGoing -> Res.string.event_not_going
            Rsvp.Status.Going,
            Rsvp.Status.Unknown,
            -> Res.string.event_going
        },
    )
    is ExternalRecordStateHolders.Comments -> stringResource(Res.string.event_comments)
}

@Composable
private fun PublicationHeader(
    publication: StandardPublication,
    actions: (Action) -> Unit,
) {
    publication.description
        ?.takeIf(String::isNotBlank)
        ?.let { description ->
            RecordDescription(
                title = publication.name,
                description = description,
                actions = actions,
            )
        }
}

@Composable
internal fun RecordDescription(
    title: String,
    description: String,
    actions: (Action) -> Unit,
) {
    val fullDescriptionDialogState = rememberSimpleDialogState()
    var isTruncated by remember(description) { mutableStateOf(false) }

    val textLinks = remember(description) {
        AnnotatedString(description).links()
    }
    val annotatedText = rememberFormattedTextPost(
        text = description,
        textLinks = textLinks,
        onLinkTargetClicked = navigableLinkTargetHandler { navigable ->
            fullDescriptionDialogState.hide()
            actions(
                Action.Navigate.To(
                    pathDestination(
                        path = navigable.path,
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
    )
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        modifier = Modifier
            .padding(
                vertical = 8.dp,
            ),
    ) {
        Text(
            text = annotatedText,
            style = textStyle,
            maxLines = DescriptionMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                isTruncated = textLayoutResult.hasVisualOverflow
            },
        )
        if (isTruncated) Text(
            modifier = Modifier
                .padding(
                    top = 4.dp,
                )
                .clickable(
                    onClick = fullDescriptionDialogState::show,
                ),
            text = stringResource(Res.string.see_more),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    SimpleDialog(
        state = fullDescriptionDialogState,
        title = {
            SimpleDialogTitle(
                text = title,
            )
        },
        text = {
            Text(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                text = annotatedText,
                style = textStyle,
            )
        },
        confirmButton = {
            NeutralDialogButton(
                text = stringResource(CommonStrings.close),
                onClick = fullDescriptionDialogState::hide,
            )
        },
    )
}

private const val ExternalRecordSharedElementPrefix = "external-record"

private const val DescriptionMaxLines = 4
