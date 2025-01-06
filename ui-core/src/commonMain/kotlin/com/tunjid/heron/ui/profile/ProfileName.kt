package com.tunjid.heron.ui.profile

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import com.tunjid.heron.data.core.models.Profile

@Composable
fun ProfileName(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Text(
        modifier = modifier,
        text = remember(profile.displayName) {
            profile.displayName ?: ""
        },
        maxLines = 1,
        style = LocalTextStyle.current.copy(fontWeight = Bold),
    )
}

@Composable
fun ProfileHandle(
    modifier: Modifier = Modifier,
    profile: Profile,
) {
    Text(
        modifier = modifier,
        text = remember(profile.handle) {
            profile.handle.id
        },
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}