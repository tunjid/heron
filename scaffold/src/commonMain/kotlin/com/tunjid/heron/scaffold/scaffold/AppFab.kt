package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
    ExtendedFloatingActionButton(
        modifier = modifier
            .animateContentSize()
            .sharedElement(
                key = FabSharedElementKey,
                zIndexInOverlay = FabSharedElementZIndex,
            ),
        onClick = onClick,
        expanded = expanded,
        shape = RoundedCornerShape(64.dp),
        text = {
            Text(
                text = text,
                maxLines = 1,
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        }
    )
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
