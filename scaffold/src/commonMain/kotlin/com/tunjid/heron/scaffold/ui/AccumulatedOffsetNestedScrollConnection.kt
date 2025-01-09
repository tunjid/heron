package com.tunjid.heron.scaffold.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import com.tunjid.heron.scaffold.scaffold.BottomNavHeight
import kotlin.math.max
import kotlin.math.min

@Stable
class AccumulatedOffsetNestedScrollConnection(
    private val invert: Boolean = false,
    private val maxOffset: Offset,
    private val minOffset: Offset,
    initialOffset: Offset = Offset.Zero,
) : NestedScrollConnection {

    var offset by mutableStateOf(initialOffset)
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

@Composable
fun rememberAccumulatedOffsetNestedScrollConnection(
    invert: Boolean = false,
    maxOffset: Density.() -> Offset,
    minOffset: Density.() -> Offset,
): AccumulatedOffsetNestedScrollConnection {
    var savedOffset by rememberSaveable {
        mutableLongStateOf(0L)
    }
    val density = LocalDensity.current
    val connection = remember {
        AccumulatedOffsetNestedScrollConnection(
            invert = invert,
            maxOffset = density.maxOffset(),
            minOffset = density.minOffset(),
            initialOffset = Offset(
                x = unpackFloat1(savedOffset),
                y = unpackFloat2(savedOffset)
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            savedOffset = packFloats(
                val1 = connection.offset.x,
                val2 = connection.offset.y,
            )
        }
    }
    return connection
}