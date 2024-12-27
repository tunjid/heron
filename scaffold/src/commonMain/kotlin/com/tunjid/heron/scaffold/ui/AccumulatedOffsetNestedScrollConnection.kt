package com.tunjid.heron.scaffold.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

class AccumulatedOffsetNestedScrollConnection(
    private val invert: Boolean = false,
    private val maxY: Float,
    private val minY: Float,
) : NestedScrollConnection {

    var offset by mutableStateOf(Offset.Zero)
        private set

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val adjusted =
            if (invert) offset - available
            else offset + available

        offset = adjusted.copy(
            y = max(
                min(adjusted.y, maxY),
                minY,
            )
        )


        return Offset.Zero
    }
}

@Composable
fun bottomAppBarAccumulatedOffsetNestedScrollConnection(): AccumulatedOffsetNestedScrollConnection {
    val density = LocalDensity.current
    return remember(density) {
        AccumulatedOffsetNestedScrollConnection(
            invert = true,
            maxY = with(density) {
                WindowInsets.navigationBars.run {
                    getTop(density) + getBottom(density)
                } + 64.dp.toPx()
            },
            minY = 0f,
        )
    }
}