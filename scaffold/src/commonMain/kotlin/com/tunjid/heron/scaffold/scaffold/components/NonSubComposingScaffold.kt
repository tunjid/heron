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

package com.tunjid.heron.scaffold.scaffold.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.tunjid.heron.ui.platformStatusBars
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * An implementation of the material 3 scaffold that does not use subcomposition, and doesn't
 * clip its content.
 */
@Composable
fun NonSubComposingScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .background(containerColor),
    ) {
        ScaffoldLayout(
            fabPosition = floatingActionButtonPosition,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
            snackbar = snackbarHost,
            contentWindowInsets = contentWindowInsets,
            fab = floatingActionButton,
        )
    }
}

@Composable
private inline fun ScaffoldLayout(
    fabPosition: FabPosition,
    crossinline topBar: @Composable () -> Unit,
    crossinline content: @Composable (PaddingValues) -> Unit,
    crossinline snackbar: @Composable () -> Unit,
    crossinline fab: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    crossinline bottomBar: @Composable () -> Unit,
) {
    // Create the backing value for the content padding
    // These values will be updated during measurement, but before subcomposing the body content
    // Remembering and updating a single PaddingValues avoids needing to recompose when the values
    // change
    val contentPadding = remember {
        object : PaddingValues {
            var paddingHolder by mutableStateOf(PaddingValues(0.dp))

            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateLeftPadding(layoutDirection)

            override fun calculateTopPadding(): Dp = paddingHolder.calculateTopPadding()

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateRightPadding(layoutDirection)

            override fun calculateBottomPadding(): Dp = paddingHolder.calculateBottomPadding()
        }
    }

    Layout(
        contents =
        listOf(
            { Spacer(Modifier.windowInsetsTopHeight(contentWindowInsets)) },
            { Spacer(Modifier.windowInsetsBottomHeight(contentWindowInsets)) },
            { Spacer(Modifier.windowInsetsStartWidth(contentWindowInsets)) },
            { Spacer(Modifier.windowInsetsEndWidth(contentWindowInsets)) },
            { Box { topBar() } },
            { Box { snackbar() } },
            { Box { fab() } },
            { Box { bottomBar() } },
            { Box { content(contentPadding) } },
        ),
    ) { measurables, constraints ->
        val topInsetsMeasurable = measurables[0].first()
        val bottomInsetsMeasurable = measurables[1].first()
        val startInsetsMeasurable = measurables[2].first()
        val endInsetsMeasurable = measurables[3].first()
        val topBarMeasurable = measurables[4].first()
        val snackBarMeasurable = measurables[5].first()
        val fabMeasurable = measurables[6].first()
        val bottomBarMeasurable = measurables[7].first()
        val bodyMeasurable = measurables[8].first()

        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val topInsetsPlaceable = topInsetsMeasurable.measure(looseConstraints)
        val bottomInsetsPlaceable = bottomInsetsMeasurable.measure(looseConstraints)
        val startInsetsPlaceable = startInsetsMeasurable.measure(looseConstraints)
        val endInsetsPlaceable = endInsetsMeasurable.measure(looseConstraints)

        val startInsetsWidth = startInsetsPlaceable.width
        val endInsetsWidth = endInsetsPlaceable.width

        val leftInsetsWidth =
            if (layoutDirection == LayoutDirection.Ltr) {
                startInsetsWidth
            } else {
                endInsetsWidth
            }
        val rightInsetsWidth =
            if (layoutDirection == LayoutDirection.Ltr) {
                endInsetsWidth
            } else {
                startInsetsWidth
            }

        val topBarPlaceable = topBarMeasurable.measure(looseConstraints)

        val topBarHeight = topBarPlaceable.height

        val snackbarPlaceable =
            snackBarMeasurable.measure(
                looseConstraints.offset(
                    -startInsetsPlaceable.width - endInsetsPlaceable.width,
                    -bottomInsetsPlaceable.height,
                ),
            )

        val snackbarHeight = snackbarPlaceable.height
        val snackbarWidth = snackbarPlaceable.width

        val fabPlaceable =
            fabMeasurable.measure(
                looseConstraints.offset(
                    -startInsetsPlaceable.width - endInsetsPlaceable.width,
                    -bottomInsetsPlaceable.height,
                ),
            )

        val isFabEmpty = fabPlaceable.width == 0 && fabPlaceable.height == 0
        val fabPlacement =
            if (!isFabEmpty) {
                val fabWidth = fabPlaceable.width
                val fabHeight = fabPlaceable.height
                // FAB distance from the left of the layout, taking into account LTR / RTL
                val fabLeftOffset =
                    when (fabPosition) {
                        FabPosition.Start -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                FabSpacing.roundToPx() + startInsetsWidth
                            } else {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - startInsetsWidth
                            }
                        }
                        FabPosition.End,
                        FabPosition.EndOverlay,
                        -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - endInsetsWidth
                            } else {
                                FabSpacing.roundToPx() + endInsetsWidth
                            }
                        }
                        else -> (layoutWidth - fabWidth + leftInsetsWidth - rightInsetsWidth) / 2
                    }

                FabPlacement(left = fabLeftOffset, width = fabWidth, height = fabHeight)
            } else {
                null
            }

        val bottomBarPlaceable = bottomBarMeasurable.measure(looseConstraints)

        val bottomBarHeight = bottomBarPlaceable.height

        val fabOffsetFromBottom =
            fabPlacement?.let {
                if (fabPosition == FabPosition.EndOverlay) {
                    bottomInsetsPlaceable.height
                } else {
                    max(bottomInsetsPlaceable.height, bottomBarHeight)
                } + it.height + FabSpacing.roundToPx()
            }
        val snackbarOffsetFromBottom =
            if (snackbarHeight != 0) {
                snackbarHeight +
                    max(
                        fabOffsetFromBottom ?: 0,
                        max(bottomBarHeight, bottomInsetsPlaceable.height),
                    )
            } else {
                0
            }

        // Update the backing value for the content padding of the body content
        // We do this before measuring or placing the body content
        contentPadding.paddingHolder =
            PaddingValues(
                top = max(topBarHeight, topInsetsPlaceable.height).toDp(),
                bottom = max(bottomBarHeight, bottomInsetsPlaceable.height).toDp(),
                start = startInsetsPlaceable.width.toDp(),
                end = endInsetsPlaceable.width.toDp(),
            )

        val bodyContentPlaceable = bodyMeasurable.measure(looseConstraints)

        layout(layoutWidth, layoutHeight) {
            // Inset spacers are just for convenient measurement logic, no need to place them
            // Placing to control drawing order to match default elevation of each placeable
            bodyContentPlaceable.place(0, 0)
            topBarPlaceable.place(0, 0)
            snackbarPlaceable.place(
                (layoutWidth - snackbarWidth + leftInsetsWidth - rightInsetsWidth) / 2,
                layoutHeight - snackbarOffsetFromBottom,
            )
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceable.place(0, layoutHeight - bottomBarHeight)
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlacement?.let { placement ->
                fabOffsetFromBottom?.let { fabOffset ->
                    fabPlaceable.place(placement.left, layoutHeight - fabOffset)
                }
            }
        }
    }
}

/** Object containing various default values for [Scaffold] component. */
object ScaffoldDefaults {
    /** Default insets to be used and consumed by the scaffold content slot */
    val contentWindowInsets: WindowInsets
        @Composable get() = WindowInsets.systemBarsForVisualComponents
}

/** The possible positions for a [FloatingActionButton] attached to a [Scaffold]. */
@JvmInline
value class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Position FAB at the bottom of the screen at the start, above the [NavigationBar] (if it
         * exists)
         */
        val Start = FabPosition(0)

        /**
         * Position FAB at the bottom of the screen in the center, above the [NavigationBar] (if it
         * exists)
         */
        val Center = FabPosition(1)

        /**
         * Position FAB at the bottom of the screen at the end, above the [NavigationBar] (if it
         * exists)
         */
        val End = FabPosition(2)

        /**
         * Position FAB at the bottom of the screen at the end, overlaying the [NavigationBar] (if
         * it exists)
         */
        val EndOverlay = FabPosition(3)
    }

    override fun toString(): String {
        return when (this) {
            Start -> "FabPosition.Start"
            Center -> "FabPosition.Center"
            End -> "FabPosition.End"
            else -> "FabPosition.EndOverlay"
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 *
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 *   support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
@Immutable
private class FabPlacement(val left: Int, val width: Int, val height: Int)

private val WindowInsets.Companion.systemBarsForVisualComponents: WindowInsets
    @Composable get() = systemBars
        .union(displayCutout)
        .union(WindowInsets.platformStatusBars)

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp
