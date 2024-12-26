package com.tunjid.heron.timeline.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TimelineTabs(
    modifier: Modifier = Modifier,
    titles: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabShape = remember { RoundedCornerShape(16.dp) }
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        edgePadding = 28.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions: List<TabPosition> ->
            if (tabPositions.isNotEmpty()) Box(
                Modifier.tabIndicatorOffset(
                    tabPositions[selectedTabIndex]
                )
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = tabShape,
                    )
                    .height(2.dp)
                    .offset(y = 46.dp)
            )
        },
        divider = { }
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedTabIndex
            val textModifier = Modifier
                .padding(vertical = 4.dp)
            Tab(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(tabShape),
                selected = selected,
                onClick = {
                    onTabSelected(index)
                },
                content = {
                    Text(
                        modifier = textModifier,
                        text = title,
                    )
                },
            )
        }
    }
}