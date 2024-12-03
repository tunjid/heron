package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane

@Composable
fun Modifier.predictiveBackBackgroundModifier(
    paneScope: PaneScope<ThreePane, *>
): Modifier {
    if (paneScope.paneState.pane != ThreePane.TransientPrimary)
        return this

    var elevation by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
        ) { value, _ -> elevation = value.dp }
    }
    return background(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
        shape = RoundedCornerShape(16.dp)
    )
        .clip(RoundedCornerShape(16.dp))

}
