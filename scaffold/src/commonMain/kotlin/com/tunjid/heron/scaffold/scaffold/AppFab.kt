package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.ui.SharedElementScope
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.create
import heron.scaffold.generated.resources.notifications
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppFab(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
    text: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
) = with(sharedElementScope) {
    ExtendedFloatingActionButton(
        modifier = modifier
            .sharedElement(
                key = "Fab"
            ),
        onClick = onClick,
        expanded = expanded,
        text = {
            Text(text)
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
    AppFab(
        modifier = modifier,
        sharedElementScope = sharedElementScope,
        onClick = onClick,
        text = stringResource(Res.string.create),
        icon = Icons.Rounded.Edit,
        expanded = expanded,
    )
}