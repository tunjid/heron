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
    private val maxOffset: Offset,
    private val minOffset: Offset,
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
            x = max(
                min(adjusted.x, maxOffset.x),
                minOffset.x,
            ),
            y = max(
                min(adjusted.y, maxOffset.y),
                minOffset.y,
            ),
        )


        return Offset.Zero
    }
}

@Composable
fun bottomAppBarAccumulatedOffsetNestedScrollConnection(): AccumulatedOffsetNestedScrollConnection {
    val density = LocalDensity.current
    val navigationBarInsets = WindowInsets.navigationBars
    return remember(density, navigationBarInsets) {
        AccumulatedOffsetNestedScrollConnection(
            invert = true,
            maxOffset = Offset(
                x = 0f,
                y = with(density) {
                    navigationBarInsets.run {
                        getTop(density) + getBottom(density)
                    } + 80.dp.toPx()
                }
            ),
            minOffset = Offset.Zero,
        )
    }
}