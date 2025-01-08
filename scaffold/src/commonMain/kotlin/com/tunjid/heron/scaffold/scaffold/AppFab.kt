package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.SharedElementScope
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.post
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
            .wrapContentHeight()
            .animateContentSize()
            .sharedElement(
                key = FabSharedElementKey,
                zIndexInOverlay = FabSharedElementZIndex,
            ),
        onClick = onClick,
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        content = {
            Row(
                modifier = Modifier
                    .animateContentSize()
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
