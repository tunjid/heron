package com.tunjid.heron.scaffold.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.tunjid.composables.accumulatedoffsetnestedscrollConnection.AccumulatedOffsetNestedScrollConnection
import com.tunjid.composables.accumulatedoffsetnestedscrollConnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.scaffold.scaffold.BottomNavHeight


@Composable
fun bottomNavigationNestedScrollConnection(): AccumulatedOffsetNestedScrollConnection {
    val navigationBarInsets = WindowInsets.navigationBars
    return rememberAccumulatedOffsetNestedScrollConnection(
        invert = true,
        maxOffset = maxOffset@{
            Offset(
                x = 0f,
                y = navigationBarInsets.run {
                    getTop(this@maxOffset) + getBottom(this@maxOffset)
                } + BottomNavHeight.toPx()
            )
        },
        minOffset = { Offset.Zero },
    )
}
