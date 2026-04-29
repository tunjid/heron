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

package com.tunjid.heron.editprofile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.editprofile.EditProfileScreenTabs
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.tabIndex
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberEditProfileTabs(
    tabs: List<EditProfileScreenTabs>,
) = tabs.map {
    Tab(title = stringResource(it.stringResource), hasUpdate = false)
}

@Composable
fun EditProfileTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: List<Tab>,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tabs(
            modifier = Modifier
                .animateContentSize()
                .weight(1f)
                .clip(CircleShape),
            tabsState = rememberTabsState(
                tabs = tabs,
                selectedTabIndex = pagerState::tabIndex,
                onTabSelected = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                onTabReselected = {},
            ),
        )
    }
}
