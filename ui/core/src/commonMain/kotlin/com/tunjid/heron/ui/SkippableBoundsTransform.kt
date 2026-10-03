package com.tunjid.heron.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.geometry.Rect
import com.tunjid.composables.ui.skipIf

fun skippableBoundsTransform(
    delegate: BoundsTransform,
    skip: () -> Boolean,
): BoundsTransform = SkippableBoundsTransform(
    delegate,
    skip,
)

private class SkippableBoundsTransform(
    private val delegate: BoundsTransform,
    private val skip: () -> Boolean,
) : BoundsTransform {
    override fun createAnimationSpec(
        initialBounds: Rect,
        targetBounds: Rect,
    ): FiniteAnimationSpec<Rect> = delegate.createAnimationSpec(
        initialBounds,
        targetBounds,
    ).skipIf(skip)
}
