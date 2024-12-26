package com.tunjid.heron.timeline.ui.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedTabIndex
            FilterChip(
                modifier = Modifier
                    .padding(horizontal = 4.dp),
                shape = TabShape,
                border = if (selected) FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = true,
                ) else null,
                selected = selected,
                onClick = {
                    onTabSelected(index)
                },
                label = {
                    Text(
                        text = title,
                    )
                },
            )
        }
    }
}

val TabShape = RoundedCornerShape(16.dp)
