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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimelineTabs(
    modifier: Modifier = Modifier,
    titles: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions: List<TabPosition> ->
       if(tabPositions.isNotEmpty())     Box(
                Modifier.tabIndicatorOffset(
                    tabPositions[selectedTabIndex]
                )
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp)
                    )

                    .height(2.dp)
                    .offset(y = 46.dp)

                    .padding(horizontal = 4.dp)

//                    .border(
//                        border = BorderStroke(
//                            width = 1.dp,
//                            color = MaterialTheme.colorScheme.onSurface,
//                        ),
//                        shape = RoundedCornerShape(16.dp)
//                    )
            )
//            {
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.BottomStart)
//                        .matchParentSize()
//                        .height(2.dp)
//                        .background(color = MaterialTheme.colorScheme.onSurface)
//                        .clip(RoundedCornerShape(16.dp))
//
//                )
//            }
        },
        divider = { }
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedTabIndex
            val textModifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
            Tab(
                modifier = Modifier
                    .padding(horizontal = 4.dp),
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