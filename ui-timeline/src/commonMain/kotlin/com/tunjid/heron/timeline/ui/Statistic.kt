package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.timeline.utilities.format

@Composable
fun Statistic(
  value: Long,
  description: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text(
      modifier = Modifier.alignByBaseline(),
      text = format(value),
      maxLines = 1,
      style = LocalTextStyle.current.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      ),
    )
    Text(
      modifier = Modifier.alignByBaseline(),
      text = description,
      maxLines = 1,
      style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.outline),
    )
  }
}