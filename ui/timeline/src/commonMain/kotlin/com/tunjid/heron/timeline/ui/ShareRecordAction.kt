package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShareRecordAction(
    onShareClicked: () -> Unit,
){
    ElevatedCard(
        shape = CircleShape,
        modifier = Modifier
            .padding(horizontal = 3.dp)
    ) {
        IconButton(
            onClick = onShareClicked,
            modifier = Modifier
                .size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowCircleUp,
                contentDescription = stringResource(Res.string.more_options),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
