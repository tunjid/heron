package com.tunjid.heron.timeline.ui.profile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileRelationship
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.edit
import heron.ui_timeline.generated.resources.follow
import heron.ui_timeline.generated.resources.following
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileRelationship(
    relationship: ProfileRelationship?,
    isSignedInProfile: Boolean,
    onClick: () -> Unit,
) {
    val follows = relationship?.follows == true
    val followStatusText = stringResource(
        if (isSignedInProfile) Res.string.edit
        else if (follows) Res.string.following
        else Res.string.follow
    )
    FilterChip(
        selected = follows,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
            Icon(
                imageVector =
                if (isSignedInProfile) Icons.Rounded.Edit
                else if (follows) Icons.Rounded.Check
                else Icons.Rounded.Add,
                contentDescription = followStatusText,
            )
        },
        label = {
            Text(followStatusText)
        },
    )
}