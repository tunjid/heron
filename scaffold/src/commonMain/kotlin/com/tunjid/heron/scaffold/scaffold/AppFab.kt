package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.SharedElementScope
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.post
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Fab(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    text: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
) = with(sharedElementScope) {
    // The material3 ExtendedFloatingActionButton does not allow for placing
    // Modifier.animateContentSize() on its row.
    FloatingActionButton(
        modifier = modifier
            .animateFabSize()
            .sharedElement(
                key = FabSharedElementKey,
                zIndexInOverlay = FabSharedElementZIndex,
            ),
        onClick = onClick,
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        content = {
            Row(
                modifier = Modifier
                    .animateFabSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FabIcon(icon)
                if (expanded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(targetState = text) { text ->
                        Text(
                            text = text,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun FabIcon(icon: ImageVector) {
    val rotationAnimatable = remember { Animatable(initialValue = 0f) }
    val animationSpec = remember {
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = 0.1f
        )
    }

    Icon(
        modifier = Modifier.rotate(rotationAnimatable.value),
        imageVector = icon,
        contentDescription = null
    )

    LaunchedEffect(icon) {
        rotationAnimatable.animateTo(targetValue = 30f, animationSpec = animationSpec)
        rotationAnimatable.animateTo(targetValue = 0f, animationSpec = animationSpec)
    }
}

@Composable
fun ComposeFab(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Fab(
        modifier = modifier,
        sharedElementScope = sharedElementScope,
        onClick = onClick,
        text = stringResource(Res.string.post),
        icon = Icons.Rounded.Edit,
        expanded = expanded,
    )
}

@Composable
fun isFabExpanded(offset: Offset): Boolean {
    val density = LocalDensity.current
    var result by remember { mutableStateOf(true) }
    val updatedOffset by rememberUpdatedState(offset)
    LaunchedEffect(Unit) {
        snapshotFlow {
            updatedOffset.y < with(density) { 56.dp.toPx() }
        }
            .collect {
                result = it
            }
    }
    return result
}

private data object FabSharedElementKey

// Modifier for animating fab without the clipping caused by modifier.animateBounds.
// Consider replacing everything below this with Modifier.animateBounds once stable.
// See the following convo for why this is necessary:
// https://slack-chats.kotlinlang.org/t/507386/is-there-an-animatecontentsize-modifier-that-does-not-clip-t
private fun Modifier.animateFabSize(
    animationSpec: FiniteAnimationSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntSize.VisibilityThreshold
    ),
    alignment: Alignment = Alignment.TopStart,
    finishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)? = null,
): Modifier =
    this then
            SizeAnimationModifierElement(
                animationSpec,
                alignment,
                finishedListener
            )

private data class SizeAnimationModifierElement(
    val animationSpec: FiniteAnimationSpec<IntSize>,
    val alignment: Alignment,
    val finishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)?,
) : ModifierNodeElement<SizeAnimationModifierNode>() {
    override fun create(): SizeAnimationModifierNode =
        SizeAnimationModifierNode(animationSpec, alignment, finishedListener)

    override fun update(node: SizeAnimationModifierNode) {
        node.animationSpec = animationSpec
        node.listener = finishedListener
        node.alignment = alignment
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateContentSize"
        properties["animationSpec"] = animationSpec
        properties["alignment"] = alignment
        properties["finishedListener"] = finishedListener
    }
}

internal val InvalidSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
internal val IntSize.isValid: Boolean
    get() = this != InvalidSize

/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
private class SizeAnimationModifierNode(
    var animationSpec: AnimationSpec<IntSize>,
    var alignment: Alignment = Alignment.TopStart,
    var listener: ((startSize: IntSize, endSize: IntSize) -> Unit)? = null,
) : LayoutModifierNodeWithPassThroughIntrinsics() {
    private var lookaheadSize: IntSize = InvalidSize
    private var lookaheadConstraints: Constraints = Constraints()
        set(value) {
            field = value
            lookaheadConstraintsAvailable = true
        }
    private var lookaheadConstraintsAvailable: Boolean = false

    private fun targetConstraints(default: Constraints) =
        if (lookaheadConstraintsAvailable) {
            lookaheadConstraints
        } else {
            default
        }

    data class AnimData(
        val anim: Animatable<IntSize, AnimationVector2D>,
        var startSize: IntSize,
    )

    var animData: AnimData? by mutableStateOf(null)

    override fun onReset() {
        super.onReset()
        // Reset is an indication that the node may be re-used, in such case, animData becomes stale
        animData = null
    }

    override fun onAttach() {
        super.onAttach()
        // When re-attached, we may be attached to a tree without lookahead scope.
        lookaheadSize = InvalidSize
        lookaheadConstraintsAvailable = false
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = if (isLookingAhead) {
            lookaheadConstraints = constraints
            measurable.measure(constraints)
        } else {
            // Measure with lookahead constraints when available, to avoid unnecessary relayout
            // in child during the lookahead animation.
            measurable.measure(targetConstraints(constraints))
        }
        val measuredSize = IntSize(placeable.width, placeable.height)
        val (width, height) = if (isLookingAhead) {
            lookaheadSize = measuredSize
            measuredSize
        } else {
            animateTo(if (lookaheadSize.isValid) lookaheadSize else measuredSize).let {
                // Constrain the measure result to incoming constraints, so that parent doesn't
                // force center this layout.
                constraints.constrain(it)
            }
        }
        return layout(width, height) {
            val offset =
                alignment.align(
                    size = measuredSize,
                    space = IntSize(width, height),
                    layoutDirection = this@measure.layoutDirection
                )
            placeable.place(offset)
        }
    }

    fun animateTo(targetSize: IntSize): IntSize {
        val data = animData?.apply {
            // TODO(b/322878517): Figure out a way to seamlessly continue the animation after
            //  re-attach. Note that in some cases restarting the animation is the correct behavior.
            val wasInterrupted = (targetSize != anim.value && !anim.isRunning)

            if (targetSize != anim.targetValue || wasInterrupted) {
                startSize = anim.value
                coroutineScope.launch {
                    val result = anim.animateTo(targetSize, animationSpec)
                    if (result.endReason == AnimationEndReason.Finished) {
                        listener?.invoke(startSize, result.endState.value)
                    }
                }
            }
        } ?: AnimData(
            Animatable(
                targetSize, IntSize.VectorConverter, IntSize(1, 1)
            ),
            targetSize
        )

        animData = data
        return data.anim.value
    }
}

private abstract class LayoutModifierNodeWithPassThroughIntrinsics :
    LayoutModifierNode, Modifier.Node() {
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ) = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ) = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ) = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ) = measurable.maxIntrinsicHeight(width)
}

