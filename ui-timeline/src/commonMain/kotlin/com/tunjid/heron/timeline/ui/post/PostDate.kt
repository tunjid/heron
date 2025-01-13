package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.timeline.utilities.formatDate
import com.tunjid.heron.timeline.utilities.formatTime
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.like
import heron.ui_timeline.generated.resources.likes
import heron.ui_timeline.generated.resources.quote
import heron.ui_timeline.generated.resources.quotes
import heron.ui_timeline.generated.resources.repost
import heron.ui_timeline.generated.resources.reposts
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostMetadata(
    modifier: Modifier = Modifier,
    time: Instant,
    reposts: Long,
    quotes: Long,
    likes: Long,
) {
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.outline
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = modifier,
            text = "${time.formatDate()} • ${time.formatTime()}",
            style = textStyle,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetaDataText(
                count = reposts,
                singularResource = Res.string.repost,
                pluralResource = Res.string.reposts,
                textStyle = textStyle,
            )
            MetaDataText(
                count = quotes,
                singularResource = Res.string.quote,
                pluralResource = Res.string.quotes,
                textStyle = textStyle,
            )
            MetaDataText(
                count = likes,
                singularResource = Res.string.like,
                pluralResource = Res.string.likes,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
internal fun MetaDataText(
    count: Long,
    singularResource: StringResource,
    pluralResource: StringResource,
    textStyle: TextStyle,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = count.toString(),
            style = textStyle.copy(
                fontWeight = FontWeight.Bold,
            )
        )
        Text(
            text = stringResource(
                if (count == 1L) singularResource
                else pluralResource
            ),
            style = textStyle,
        )
    }
}